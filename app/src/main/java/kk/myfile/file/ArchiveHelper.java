package kk.myfile.file;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import de.innosystec.unrar.Archive;

import kk.myfile.R;
import kk.myfile.file.Tree.IProgressCallback;
import kk.myfile.file.Tree.ProgressType;
import kk.myfile.leaf.Direct;
import kk.myfile.leaf.Leaf;
import kk.myfile.util.DataUtil;
import kk.myfile.util.Logger;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;

public class ArchiveHelper {
	private Object mArchive;
	
	public class FileHeader {
		private Leaf mLeaf;
		private Object mHeader;
		
		private FileHeader() {
		}
		
		public Leaf getLeaf() {
			return mLeaf;
		}
		
		public Object getHeader() {
			return mHeader;
		}
		
		public long getCompressSize() {
			if (mHeader instanceof net.lingala.zip4j.model.FileHeader) {
				return ((net.lingala.zip4j.model.FileHeader) mHeader).getCompressedSize();
			} else if (mHeader instanceof de.innosystec.unrar.rarfile.FileHeader) {
				return ((de.innosystec.unrar.rarfile.FileHeader) mHeader).getFullPackSize();
			}
			
			return -1;
		}
		
		public long getExtractSize() {
			if (mHeader instanceof net.lingala.zip4j.model.FileHeader) {
				return ((net.lingala.zip4j.model.FileHeader) mHeader).getUncompressedSize();
			} else if (mHeader instanceof de.innosystec.unrar.rarfile.FileHeader) {
				return ((de.innosystec.unrar.rarfile.FileHeader) mHeader).getFullUnpackSize();
			}
			
			return -1;
		}
	}
	
	private final Map<String, FileHeader> mMap = new HashMap<>();
	
	public ArchiveHelper() {
	}
	
	public boolean setFile(String path) {
		mArchive = null;

		try {
			ZipFile archive = new ZipFile(path);

			if (archive.isValidZipFile()) {
				archive.setRunInThread(false);
				mArchive = archive;
				return true;
			}
		} catch (Exception e) {
			Logger.print(e);
		}
		
		if (mArchive == null) {
			try {
				File file = new File(path);
				Archive archive = new Archive(file, null, true);
				if (archive.isEncrypted()) {
					archive.close();
					throw new Exception("do not support rar with password");
				}
				archive.close();
				
				mArchive = new Archive(file, null, false);
				return true;
			} catch (Exception e) {
				Logger.print(e);
			}
		}
		
		return false;
	}
	
	public boolean isEncrypted() {
		try {
			if (mArchive instanceof ZipFile) {
				return ((ZipFile) mArchive).isEncrypted();
			} else if (mArchive instanceof Archive) {
				return ((Archive) mArchive).isEncrypted();
			}
		} catch (Exception e) {
			Logger.print(e);
		}
		
		return false;
	}
	
	public void setPassword(String password) {
		try {
			if (mArchive instanceof ZipFile) {
				((ZipFile) mArchive).setPassword(password);
			} else if (mArchive instanceof Archive) {
				throw new Exception("do not support rar with password");
			}
		} catch (Exception e) {
			Logger.print(e);
		}
	}
	
	public boolean parseFileHeader() {
		try {
			List<?> headers = null;
			if (mArchive instanceof ZipFile) {
				headers = ((ZipFile) mArchive).getFileHeaders();
			} else if (mArchive instanceof Archive) {
				headers = ((Archive) mArchive).getFileHeaders();
			} else {
				return false;
			}
			
			mMap.clear();
			
			for (Object obj : headers) {
				String path = null;
				boolean isDirect = false;
				Object header = null;
				
				if (mArchive instanceof ZipFile) {
					net.lingala.zip4j.model.FileHeader fh = (net.lingala.zip4j.model.FileHeader) obj;
					path = fh.getFileName();
					isDirect = fh.isDirectory();
					header = fh;
				} else if (mArchive instanceof Archive) {
					de.innosystec.unrar.rarfile.FileHeader fh = (de.innosystec.unrar.rarfile.FileHeader) obj;
					path = fh.getFileNameString();
					isDirect = fh.isDirectory();
					header = fh;
				} else {
					return false;
				}
				
				path = path.replace('\\', '/');
				
				if (path.length() < 1 || path.charAt(0) != '/') {
					path = '/' + path;
				}
				
				if (path.charAt(path.length() - 1) == '/') {
					path = path.substring(0, path.length() - 1);
				}
				
				FileHeader fh;
				
				if (isDirect) {
					fh = mMap.get(path);
					
					if (fh == null) {
						fh = new FileHeader();
						mMap.put(path, fh);
						
						fh.mLeaf =  new Direct(path);
					} else {
						fh.mHeader = header;
						continue;
					}
				} else {
					fh = new FileHeader();
					mMap.put(path, fh);
					
					fh.mLeaf = FileUtil.createTempLeaf(path);
				}
				
				fh.mHeader = header;
				
				for(Leaf leaf = fh.getLeaf(); !"/".equals(path);) {
					int ni = -1;
					for (int i = path.length() - 2; i > 0; i--) {
						if (path.charAt(i) == '/') {
							ni = i;
							break;
						}
					}
					
					boolean finish = false;
					
					String pp;
					if (ni < 1) {
						pp = "/";
						finish = true;
					} else {
						pp = path.substring(0, ni);
					}
					
					FileHeader parent = mMap.get(pp);
					if (parent == null) {
						parent = new FileHeader();
						mMap.put(pp, parent);
						
						parent.mLeaf = new Direct(pp);
					} else {
						finish = true;
					}
					
					((Direct) parent.getLeaf()).getChildren().add(leaf);
					
					if (finish) {
						break;
					}
					
					path = pp;
					leaf = parent.getLeaf();
				}
			}
			
			return true;
		} catch (Exception e) {
			Logger.print(e);
		}
		
		return false;
	}
	
	public FileHeader getFileHeader(String path) {
		return mMap.get(path);
	}
	
	public File extractFile(FileHeader fh, String dir) {
		try {
			if (mArchive instanceof ZipFile) {
				net.lingala.zip4j.model.FileHeader header =
					(net.lingala.zip4j.model.FileHeader) fh.getHeader();
				((ZipFile) mArchive).extractFile(header, dir);
				
				return new File(dir, header.getFileName());
			} else if (mArchive instanceof Archive) {
				de.innosystec.unrar.rarfile.FileHeader header =
					(de.innosystec.unrar.rarfile.FileHeader) fh.getHeader();
				
				String name = DataUtil.getFileName(header.getFileNameString());
				File file = new File(dir, name);
				FileUtil.createFile(file);
				FileOutputStream fos = new FileOutputStream(file);
				
				((Archive) mArchive).extractFile(header, fos);
				fos.close();
				
				return file;
			}
		} catch (Exception e) {
			Logger.print(e);
		}
		
		return null;
	}
	
	public void extractFile(String dir, AtomicBoolean cancel, IProgressCallback callback) {
		try {
			if (mArchive instanceof ZipFile) {
				ZipFile zf = (ZipFile) mArchive;
				zf.setRunInThread(true);
				zf.extractAll(dir);
				
				ProgressMonitor pm = zf.getProgressMonitor();
				
				Tree.monitorZip(false, pm, cancel, callback);
				
				return;
			}
			
			if (mArchive instanceof Archive) {
				Archive archive = (Archive) mArchive;
				List<de.innosystec.unrar.rarfile.FileHeader> headers = archive.getFileHeaders();
				int size = headers.size();
				
				for (int i = 0; i < size; i++) {
					if (cancel != null && cancel.get()) {
						if (callback != null) {
							callback.onProgress(ProgressType.Cancel);
						}
						return;
					}
					
					String err = null;
					String path = "";
					
					try {
						de.innosystec.unrar.rarfile.FileHeader header = headers.get(i);
						path = header.getFileNameString();
						path = path.replace('\\', '/');
						File file = new File(dir, path);
						
						if (header.isDirectory()) {
							err = FileUtil.createDirect(file);
						} else {
							err = FileUtil.createFile(file);
							FileOutputStream fos = new FileOutputStream(file);
							archive.extractFile(header, fos);
							fos.close();
						}
					} catch (Exception e) {
						Logger.print(e);
						err = e.toString();
					}
					
					if (err == null) {
						if (callback != null) {
							callback.onProgress(ProgressType.Progress, path, i, size);
						}
					} else {
						if (callback != null) {
							callback.onProgress(ProgressType.Error, err);
						}
					}
				}
				
				if (callback != null) {
					callback.onProgress(ProgressType.Finish);
				}
				
				return;
			}
		} catch (Exception e) {
			Logger.print(e);
		}
			
		if (callback != null) {
			callback.onProgress(ProgressType.Error, R.string.err_extract_failed);
		}
	}
}
