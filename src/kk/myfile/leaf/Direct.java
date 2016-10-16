package kk.myfile.leaf;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import kk.myfile.R;
import kk.myfile.file.FileUtil;
import kk.myfile.file.Tree;

public class Direct extends Leaf {
	protected final List<Leaf> mChildren = new ArrayList<Leaf>();
	
	public Direct(String path) {
		super(path);
	}
	
	@Override
	public int getIcon() {
		return R.drawable.file_directory;
	}
	
	public void loadChildren(boolean visible) {
		try {
			synchronized (mChildren) {
				mChildren.clear();
			
				for (File file : getFile().listFiles()) {
					if (visible && Tree.HIDDEN_FILE.equals(file.getName())) {
						mChildren.clear();
						return;
					}
					
					if (visible == false || file.isHidden() == false) {
						mChildren.add(FileUtil.createLeaf(file));
					}
				}
			}
		} catch (Exception e) {
		}
	}
	
	public void loadChildrenRec(boolean visible, boolean canon) {
		Queue<Direct> queue = new LinkedList<Direct>();
		queue.add(this);

loop:	for (Direct dir = queue.poll(); dir != null; dir = queue.poll()) {
			try {
				File parent = dir.getFile();
				
				if (canon && FileUtil.isLink(parent)) {
					continue;
				}
				
				List<Leaf> children = new ArrayList<Leaf>();
				
				for (File file : parent.listFiles()) {
					if (visible && Tree.HIDDEN_FILE.equals(file.getName())) {
						continue loop;
					}
					
					if (visible == false || file.isHidden() == false) {
						children.add(FileUtil.createLeaf(file));
					}
				}
				
				synchronized (dir.mChildren) {
					dir.mChildren.clear();
					dir.mChildren.addAll(children);
				}
				
				for (Leaf leaf : children) {
					if (leaf instanceof Direct) {
						queue.offer((Direct) leaf);
					}
				}
			} catch (Exception e) {
			}
		}
	}
	
	public List<Leaf> getChildren() {
		return mChildren;
	}
}
