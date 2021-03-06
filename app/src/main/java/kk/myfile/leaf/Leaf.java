package kk.myfile.leaf;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;

import kk.myfile.R;
import kk.myfile.activity.DirectActivity;
import kk.myfile.adapter.DetailItemAdapter.Data;
import kk.myfile.adapter.DetailItemAdapter.IClickListener;
import kk.myfile.adapter.DetailItemAdapter.ViewHolder;
import kk.myfile.util.AppUtil;
import kk.myfile.util.DataUtil;
import kk.myfile.util.IntentUtil;
import kk.myfile.util.IntentUtil.IAsListener;
import kk.myfile.util.Logger;
import kk.myfile.util.MathUtil;
import kk.myfile.util.Setting;

public abstract class Leaf {
	protected final String mPath;

	private Object mTag;

	public Leaf(String path) {
		if (path == null) {
			mPath = "/";
		} else {
			mPath = path;
		}
	}

	public String getPath() {
		return mPath;
	}

	public File getFile() {
		return new File(mPath);
	}

	public void setTag(Object tag) {
		mTag = tag;
	}

	public Object getTag() {
		return mTag;
	}

	public abstract int getIcon();

	protected Data putDetail(List<Data> list, int sort, int key, Object value, Object... args) {
		Data data = new Data();

		if (value == null) {
			return data;
		}

		String v = String.valueOf(value);
		if (v.length() < 1) {
			return data;
		}

		if (args != null && args.length > 0) {
			v = String.format(v, args);

			if (v.length() < 1) {
				return data;
			}
		}

		data.leaf = this;
		data.sort = sort;
		data.key = AppUtil.getString(key);
		data.value = v;

		for (int i = 0; i < list.size(); i++) {
			if (sort < list.get(i).sort) {
				list.add(i, data);
				return data;
			}
		}

		list.add(data);

		return data;
	}

	public List<Data> getDetail() {
		List<Data> list = new ArrayList<>();

		int idx = mPath.lastIndexOf('/');
		final String parent = mPath.substring(0, idx);
		String name = mPath.substring(idx + 1);
		File file = new File(mPath);

		putDetail(list, 1, R.string.word_name, name);
		putDetail(list, 1, R.string.word_parent, parent).clickListener = new IClickListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onClick(Data data, ViewHolder vh) {
				Intent intent = new Intent(AppUtil.getContext(), DirectActivity.class);
				intent.putExtra(DirectActivity.KEY_PATH, parent);
				intent.putExtra(DirectActivity.KEY_CUR_CHILD, mPath);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				AppUtil.getContext().startActivity(intent);
			}
		};
		try {
			putDetail(list, 1, R.string.word_real_path, file.getCanonicalPath()).clickListener = new IClickListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onClick(Data data, ViewHolder vh) {
					try {
						File file = data.leaf.getFile().getCanonicalFile();

						Intent intent = new Intent(AppUtil.getContext(), DirectActivity.class);
						intent.putExtra(DirectActivity.KEY_PATH, file.getParent());
						intent.putExtra(DirectActivity.KEY_CUR_CHILD, file.getPath());
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						AppUtil.getContext().startActivity(intent);
					} catch (Exception e) {
						Logger.print(e);
					}
				}
			};
		} catch (Exception e) {
			Logger.print(e);
		}
		
		String clsName = getClass().getSimpleName();
		int index = clsName.lastIndexOf('.');
		String typeName = AppUtil.getString(String.format("type_%s", clsName.substring(index + 1)
			.toLowerCase(Setting.LOCALE)));
		putDetail(list, 1, R.string.word_type, typeName);

		try {
			putDetail(list, 1, R.string.word_size, "%s B", MathUtil.insertComma(file.length()));
		} catch (Exception e) {
			Logger.print(e);
		}

		try {
			Date date = new Date(file.lastModified());
			DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Setting.LOCALE);
			putDetail(list, 1, R.string.word_modify_time, df.format(date));
		} catch (Exception e) {
			Logger.print(e);
		}

		if (!(this instanceof Direct)) {
			putDetail(list, 3, R.string.word_md5, AppUtil.getString(R.string.msg_click_to_calc)).clickListener = new IClickListener() {
				@Override
				public void onClick(final Data data, final ViewHolder vh) {
					if (AppUtil.getString(R.string.msg_click_to_calc).equals(data.value)) {
						AppUtil.runOnNewThread(new Runnable() {
							@Override
							public void run() {
								try {
									byte[] bs = DataUtil.md5(new FileInputStream(data.leaf
										.getPath()));
									final String str = DataUtil.toHexStr(bs);

									AppUtil.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											if (data == vh.data) {
												data.value = str;
												vh.value.setText(str);
											}
										}
									});
								} catch (Exception e) {
									Logger.print(e);
								}
							}
						});
					}
				}
			};
		}

		return list;
	}
	
	public void open(final Context context, boolean forceSelect) {
		if (!forceSelect && IntentUtil.view(context, this, null)) {
			return;
		}
			
		IntentUtil.showAsDialog(context, R.string.word_open_as, new IAsListener() {
			@Override
			public void onSelect(String type) {
				IntentUtil.view(context, Leaf.this, type);
			}
		});
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Leaf)) {
			return false;
		}

		Leaf other = (Leaf) obj;
		return mPath.equals(other.mPath);
	}
}
