package com.fyang0321.mylaucher;

import android.support.v4.app.Fragment;

public class MyLaucherActivity extends SingleFragmentActivity {

	@Override
	public Fragment createFragment() {
		return new LaucherFragment();
	}

}
