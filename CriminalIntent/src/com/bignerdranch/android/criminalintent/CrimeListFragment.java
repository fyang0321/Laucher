package com.bignerdranch.android.criminalintent;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class CrimeListFragment extends ListFragment {
	private ArrayList<Crime> mCrimes;
	private boolean mSubtitleVisible;
	//private final String TAG = "CrimeListFragment";
	private Callbacks mCallbacks;
	
	public interface Callbacks {
		void onCrimeSelected(Crime crime);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallbacks = (Callbacks)activity;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		getActivity().setTitle(R.string.crimes_title);
		mCrimes = CrimeLab.get(getActivity()).getCrimes();
		
		CrimeAdapter adapter = new CrimeAdapter(mCrimes);
		setListAdapter(adapter);
		setRetainInstance(true);
		mSubtitleVisible = false;
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Crime c = ((CrimeAdapter)getListAdapter()).getItem(position);
//		Log.d(TAG, c.getTitle() + "was clicked");
//		Intent i = new Intent(getActivity(), CrimePagerActivity.class);
//		i.putExtra(CrimeFragment.EXTRA_CRIME_ID, c.getId());
//		startActivity(i);
		mCallbacks.onCrimeSelected(c);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		((CrimeAdapter)getListAdapter()).notifyDataSetChanged();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_crime_list, menu);
		MenuItem showSubtitle = menu.findItem(R.id.menu_item_show_subtitle);
		if (mSubtitleVisible && showSubtitle != null) {
			showSubtitle.setTitle(R.string.hide_subtitle);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		int position = info.position;
		CrimeAdapter adapter = (CrimeAdapter)getListAdapter();
		Crime crime = adapter.getItem(position);
		
		switch(item.getItemId()) {
			case R.id.menu_item_delete_crime:
				CrimeLab.get(getActivity()).deleteCrime(crime);
				adapter.notifyDataSetChanged();
				return true;
		}
		return super.onContextItemSelected(item);
	}
	
	@TargetApi(11)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, parent, savedInstanceState);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (mSubtitleVisible) {
				getActivity().getActionBar().setSubtitle(R.string.subtitle);
			}
		}
		
		ListView listView = (ListView)v.findViewById(android.R.id.list);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			registerForContextMenu(listView);
		} else {
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
				public void onItemCheckedStateChanged(ActionMode mode, int position,
						long id, boolean checked) {
					
				}
				
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					MenuInflater inflater = mode.getMenuInflater();
					inflater.inflate(R.menu.crime_list_item_context, menu);
					return true;
				}
				
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					return false;
				}
				
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					switch (item.getItemId()) {
						case R.id.menu_item_delete_crime:
							CrimeAdapter adapter = (CrimeAdapter)getListAdapter();
							CrimeLab crimeLab = CrimeLab.get(getActivity());
							for (int i = adapter.getCount() - 1; i >= 0; i--) {
								if (getListView().isItemChecked(i))
									crimeLab.deleteCrime(adapter.getItem(i));
							}
							mode.finish();
							adapter.notifyDataSetChanged();
							return true;
						default:
							return false;
					}
				}
				
				public void onDestroyActionMode(ActionMode mode) {
					
				}
			});
			
		}
		
		return v;
	}
	
	public void updateUI() {
		((CrimeAdapter)getListAdapter()).notifyDataSetChanged();
	}
	
	@TargetApi(11)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_item_new_crime:
				Crime crime = new Crime();
				CrimeLab.get(getActivity()).addCrime(crime);
//				Intent i = new Intent(getActivity(), CrimePagerActivity.class);
//				i.putExtra(CrimeFragment.EXTRA_CRIME_ID, crime.getId());
//				startActivityForResult(i, 0);
				((CrimeAdapter)getListAdapter()).notifyDataSetChanged();
				mCallbacks.onCrimeSelected(crime);
				return true;
			case R.id.menu_item_show_subtitle:
				if (getActivity().getActionBar().getSubtitle() == null) {
					getActivity().getActionBar().setSubtitle(R.string.subtitle);
					mSubtitleVisible = true;
					item.setTitle(R.string.hide_subtitle);
				} else {
					getActivity().getActionBar().setSubtitle(null);
					mSubtitleVisible = false;
					item.setTitle(R.string.show_subtitle);
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		getActivity().getMenuInflater().inflate(R.menu.crime_list_item_context, menu);
	}
	
	private class CrimeAdapter extends ArrayAdapter<Crime> {
		public CrimeAdapter(ArrayList<Crime> crimes) {
			super(getActivity(), 0, crimes);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.list_item_crime, null);
			}
			
			Crime c = getItem(position);
			
			TextView titleTextView = 
					(TextView)convertView.findViewById(R.id.crime_list_item_titleTextView);
			titleTextView.setText(c.getTitle());
			
			TextView dateTextView = 
					(TextView)convertView.findViewById(R.id.crime_list_item_dateTextView);
			dateTextView.setText(c.getDate().toString());
			
			CheckBox solvedCheckBox =
					(CheckBox)convertView.findViewById(R.id.crime_list_item_solvedCheckBox);
			solvedCheckBox.setChecked(c.isSolved());
			
			return convertView;
		}
	}
}
