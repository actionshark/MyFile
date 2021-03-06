package kk.myfile.leaf;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import kk.myfile.R;
import kk.myfile.adapter.DetailItemAdapter.Data;
import kk.myfile.file.FileUtil;
import kk.myfile.file.Tree;
import kk.myfile.util.Logger;

public class Direct extends Leaf {
	protected final List<Leaf> mChildren = new ArrayList<>();

	public Direct(String path) {
		super(path);
	}

	@Override
	public int getIcon() {
		return R.drawable.file_directory;
	}

	@Override
	public List<Data> getDetail() {
		List<Data> list = super.getDetail();

		loadChildren(false);
		putDetail(list, 2, R.string.word_children_num, mChildren.size());

		return list;
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

					if (!visible || !file.isHidden()) {
						mChildren.add(FileUtil.createLeaf(file));
					}
				}
			}
		} catch (Exception e) {
			Logger.print(e);
		}
	}

	public void loadChildren(List<Leaf> list, boolean visible, boolean canon) {
		Queue<Direct> queue = new LinkedList<>();
		queue.add(this);

		loop: for (Direct dir = queue.poll(); dir != null; dir = queue.poll()) {
			try {
				File parent = dir.getFile();

				if (canon && FileUtil.isLink(parent)) {
					continue;
				}

				List<Leaf> children = new ArrayList<>();

				for (File file : parent.listFiles()) {
					if (visible && Tree.HIDDEN_FILE.equals(file.getName())) {
						continue loop;
					}

					if (!visible || !file.isHidden()) {
						children.add(FileUtil.createLeaf(file));
					}
				}

				list.addAll(children);

				for (Leaf leaf : children) {
					if (leaf instanceof Direct) {
						queue.offer((Direct) leaf);
					}
				}
			} catch (Exception e) {
				Logger.print(e);
			}
		}
	}

	public List<Leaf> getChildren() {
		return mChildren;
	}
}
