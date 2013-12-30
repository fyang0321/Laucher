package com.bignerdranch.android.criminalintent;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

public class CrimeFragment extends Fragment {
	private static final String TAG = "CrimeFragment";
	public static final String EXTRA_CRIME_ID =
			"com.bignerdranch.android.criminalintent.crime_id";
	private static final String DIALOG_DATE = "date";
	private static final int REQUEST_DATE = 0;
	private static final int REQUEST_PHOTO = 1;
	private static final int REQUEST_CONTACT = 2;
	
	private Crime mCrime;
	private EditText mTitleField;
	private Button mDateButton;
	private CheckBox mSolvedCheckBox;
	private ImageButton mPhotoButton;
	private Button mSuspectButton;
	private ImageView mPhotoView;
	private Callbacks mCallbacks;
	
	public interface Callbacks {
		void onCrimeUpdated(Crime crime);
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
	
	public static CrimeFragment newInstance(UUID crimeId) {
		Bundle args = new Bundle();
		args.putSerializable(EXTRA_CRIME_ID, crimeId);
		
		CrimeFragment fragment = new CrimeFragment();
		fragment.setArguments(args);
		
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCrime = new Crime();
		UUID crimeId = (UUID)getArguments().getSerializable(EXTRA_CRIME_ID);
		mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
		
		setHasOptionsMenu(true);
	}
	
	public void updateDate() {
		mDateButton.setText(mCrime.getDate().toString());
	}
	
	@TargetApi(11)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_crime, parent, false);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (NavUtils.getParentActivityName(getActivity())!=null) {
				getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}
		
		mTitleField = (EditText)v.findViewById(R.id.crime_title);
		mTitleField.setText(mCrime.getTitle());
		mTitleField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(
					CharSequence c, int start, int before, int count) {
				mCrime.setTitle(c.toString());
				mCallbacks.onCrimeUpdated(mCrime);
				getActivity().setTitle(mCrime.getTitle());
			}
			
			public void beforeTextChanged(
					CharSequence c, int start, int count, int after) {
				//do not need it
			}
			
			public void afterTextChanged(Editable c) {
				//do not need it
			}
		});
		
		mDateButton = (Button)v.findViewById(R.id.crime_date);
		DateFormat format = DateFormat.getDateInstance();
		mDateButton.setText(format.format(mCrime.getDate()));
		mDateButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentManager fm = getActivity()
						.getSupportFragmentManager();
				DatePickerFragment dialog = DatePickerFragment
						.newInstance(mCrime.getDate());
				dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
				dialog.show(fm, DIALOG_DATE);
			}
		});
		
		mSolvedCheckBox = (CheckBox)v.findViewById(R.id.crime_solved);
		mSolvedCheckBox.setChecked(mCrime.isSolved());
		mSolvedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mCrime.setSolved(isChecked);
				mCallbacks.onCrimeUpdated(mCrime);
			}
		});
		
		mPhotoButton = (ImageButton)v.findViewById(R.id.crime_imageButton);
		mPhotoButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), CrimeCameraActivity.class);
				startActivityForResult(i, REQUEST_PHOTO);
			}
		});
		
		mPhotoView = (ImageView)v.findViewById(R.id.crime_imageview);
		
		Button reportButton = (Button)v.findViewById(R.id.crime_reportButton);
		reportButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
				i.putExtra(Intent.EXTRA_SUBJECT, 
						getString(R.string.crime_report_subject));
				i = Intent.createChooser(i, getString(R.string.send_report));
				startActivity(i);
			}
		});
		
		mSuspectButton = (Button)v.findViewById(R.id.crime_suspectButton);
		mSuspectButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_PICK,
						ContactsContract.Contacts.CONTENT_URI);
				startActivityForResult(i, REQUEST_CONTACT);
			}
		});
		
		if (mCrime.getSuspect() != null) {
			mSuspectButton.setText(mCrime.getSuspect());
		}
		
		PackageManager pm = getActivity().getPackageManager();
		if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) &&
				!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			mPhotoButton.setEnabled(false);
		}
		
		return v;
	}
	
	private void showPhoto() {
		Photo p = mCrime.getPhoto();
		BitmapDrawable b = null;
		if (p != null) {
			String path = getActivity()
					.getFileStreamPath(p.getFilename()).getAbsolutePath();
			b = PictureUtils.getScaledDrawable(getActivity(), path);
		}
		mPhotoView.setImageDrawable(b);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				if (NavUtils.getParentActivityIntent(getActivity())!=null) {
					NavUtils.navigateUpFromSameTask(getActivity());
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		showPhoto();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		CrimeLab.get(getActivity()).saveCrimes();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		PictureUtils.cleanImageView(mPhotoView);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) return;
		if (requestCode == REQUEST_DATE) {
			Date date = (Date)data
					.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
			mCrime.setDate(date);
			mCallbacks.onCrimeUpdated(mCrime);
			updateDate();
		} else if (requestCode == REQUEST_PHOTO) {
			String filename = data.
					getStringExtra(CrimeCameraFragment.EXTRA_PHOTO_FILENAME);
			if (filename != null) {
				Photo p = new Photo(filename);
				mCrime.setPhoto(p);
				mCallbacks.onCrimeUpdated(mCrime);
				showPhoto();
				//Log.i(TAG, "Crime: " + mCrime.getTitle() + " has a photo.");
			}
		} else if (requestCode == REQUEST_CONTACT) {
			Uri contactUri = data.getData();
			
			String[] queryFields = new String[] {
				ContactsContract.Contacts.DISPLAY_NAME	
			};
			
			Cursor c = getActivity().getContentResolver()
					.query(contactUri, queryFields, null, null, null);
			
			if (c.getCount() == 0) {
				c.close();
				return;
			}
			
			c.moveToFirst();
			String suspect = c.getString(0);
			mCrime.setSuspect(suspect);
			mCallbacks.onCrimeUpdated(mCrime);
			mSuspectButton.setText(suspect);
			c.close();
		}
	} 
	
	private String getCrimeReport() {
		String solvedString = null;
		if (mCrime.isSolved()) {
			solvedString = getString(R.string.crime_report_solved);
		} else {
			solvedString = getString(R.string.crime_report_unsolved);
		}
		
		String dateFormat = "EEE, MMM dd";
		String dateString = dateFormat.format(dateFormat, mCrime.getDate()).toString();
		
		String suspect = mCrime.getSuspect();
		if (suspect == null) {
			suspect = getString(R.string.crime_report_no_suspect);
		} else {
			suspect = getString(R.string.crime_report_suspect, suspect);
		}
		
		String report = getString(R.string.crime_report,
				mCrime.getTitle(), dateString, solvedString, suspect);
	
		return report;
	}
}
