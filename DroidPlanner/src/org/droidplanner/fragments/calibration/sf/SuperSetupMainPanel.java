package org.droidplanner.fragments.calibration.sf;

import org.droidplanner.R;
import org.droidplanner.calibration.CalParameters;
import org.droidplanner.calibration.CalParameters.OnCalibrationEvent;
import org.droidplanner.drone.Drone;
import org.droidplanner.drone.DroneInterfaces.DroneEventsType;
import org.droidplanner.drone.DroneInterfaces.OnDroneListner;
import org.droidplanner.fragments.SetupRadioFragment;
import org.droidplanner.fragments.calibration.FragmentSetupProgress;
import org.droidplanner.fragments.calibration.FragmentSetupSend;
import org.droidplanner.fragments.calibration.SetupMainPanel;
import org.droidplanner.fragments.calibration.SetupSidePanel;

import android.os.Bundle;

public abstract class SuperSetupMainPanel extends SetupMainPanel  implements
OnCalibrationEvent, OnDroneListner {

	protected Drone drone;
	protected CalParameters parameters;

	protected abstract CalParameters getParameterHandler();
	protected abstract int getInitialPanelTitle();
	protected abstract int getInitialPanelDescription();
	protected abstract void updatePanelInfo();
	protected abstract void updateCalibrationData();
	
	public SuperSetupMainPanel() {
		super();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.drone = parentActivity.drone;
		parameters = getParameterHandler();
		parameters.setOnCalibrationEventListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		doCalibrationStep(0);
	}

	@Override
	public void onResume() {
		super.onResume();
		drone.events.addDroneListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		drone.events.removeDroneListener(this);
	}

	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		switch (event) {
		case PARAMETER:
			if (parameters != null) {
				parameters.processReceivedParam();
			}
		default:
			break;
		}
	}

	@Override
	public void onReadCalibration(CalParameters calParameters) {
		doCalibrationStep(0);
		updatePanelInfo();
	}

	@Override
	public void onSentCalibration(CalParameters calParameters) {
		doCalibrationStep(0);
	}

	@Override
	public void onCalibrationData(CalParameters calParameters, int index, int count,
			boolean isSending) {
				if (sidePanel != null && parameters != null) {
					String title;
					if (isSending) {
						title = getResources().getString(
								R.string.setup_sf_desc_uploading);
					} else {
						title = getResources().getString(
								R.string.setup_sf_desc_downloading);
					}
			
					((FragmentSetupProgress) sidePanel).updateProgress(index + 1,
							count, title);
				}
			}

	@Override
	public void doCalibrationStep(int step) {
		switch (step) {
		case 3:
			uploadCalibrationData();
			break;
		case 0:
		default:
			sidePanel = getInitialPanel();
		}
	}

	private SetupSidePanel getInitialPanel() {
	
		if (parameters != null && !parameters.isParameterDownloaded()
				&& drone.MavClient.isConnected()) {
			downloadCalibrationData();
		} else {
			sidePanel = ((SetupRadioFragment) getParentFragment())
					.changeSidePanel(new FragmentSetupSend());
			sidePanel.updateTitle(getInitialPanelTitle());
			sidePanel.updateDescription(getInitialPanelDescription());
		}
		return sidePanel;
	}

	private SetupSidePanel getProgressPanel(boolean isSending) {
		sidePanel = ((SetupRadioFragment) getParentFragment())
				.changeSidePanel(new FragmentSetupProgress());
	
		if (isSending) {
			sidePanel.updateTitle(R.string.progress_title_uploading);
			sidePanel.updateDescription(R.string.progress_desc_uploading);
		} else {
			sidePanel.updateTitle(R.string.progress_title_downloading);
			sidePanel.updateDescription(R.string.progress_desc_downloading);
		}
	
		return sidePanel;
	}

	private void uploadCalibrationData() {
		if (parameters == null || !drone.MavClient.isConnected())
			return;
	
		sidePanel = getProgressPanel(true);
	
		updateCalibrationData();
		parameters.sendCalibrationParameters();
	}

	private void downloadCalibrationData() {
		if (parameters == null || !drone.MavClient.isConnected())
			return;
		sidePanel = getProgressPanel(false);
		parameters.getCalibrationParameters(drone);
	}

	protected int getSpinnerIndexFromValue(int value, int[] valueList) {
		for (int i = 0; i < valueList.length; i++) {
			if (valueList[i] == value)
				return i;
		}
		return -1;
	}

}