/*
    This file is part of InviZible Pro.

    InviZible Pro is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    InviZible Pro is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2025 by Garmatin Oleksandr invizible.soft@gmail.com
 */

package pan.alexander.tordnscrypt.tor_fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import pan.alexander.tordnscrypt.App;
import pan.alexander.tordnscrypt.MainActivity;
import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.TopFragment;
import pan.alexander.tordnscrypt.modules.ModulesStatus;
import pan.alexander.tordnscrypt.utils.root.RootExecService;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static pan.alexander.tordnscrypt.TopFragment.TorVersion;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.FAULT;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RUNNING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPED;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPING;
import static pan.alexander.tordnscrypt.utils.logger.Logger.loge;

import com.google.android.material.divider.MaterialDivider;


public class TorRunFragment extends Fragment implements TorFragmentView, View.OnClickListener,
        ViewTreeObserver.OnScrollChangedListener, View.OnTouchListener {


    private Button btnTorStart;
    private TextView tvTorStatus;
    private ProgressBar pbTor;
    private MaterialDivider divTor;
    private TextView tvTorLog;
    private ScrollView svTorLog;
    private BroadcastReceiver receiver;

    private TorFragmentPresenter presenter;


    public TorRunFragment() {
    }

    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        try {
            view = inflater.inflate(R.layout.fragment_tor_run, container, false);
        } catch (Exception e) {
            loge("TorRunFragment onCreateView", e);
            throw e;
        }

        btnTorStart = view.findViewById(R.id.btnTorStart);

        //Not required for a portrait orientation, so return
        if (btnTorStart == null) {
            return view;
        }

        btnTorStart.setOnClickListener(this);

        pbTor = view.findViewById(R.id.pbTor);
        divTor = view.findViewById(R.id.divTor);

        tvTorLog = view.findViewById(R.id.tvTorLog);

        svTorLog = view.findViewById(R.id.svTorLog);

        if (svTorLog != null) {
            svTorLog.setOnTouchListener(this);

            ViewTreeObserver observer = svTorLog.getViewTreeObserver();
            if (observer != null) {
                observer.addOnScrollChangedListener(this);
            }
        }

        tvTorStatus = view.findViewById(R.id.tvTorStatus);

        setTorLogViewText();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        //MainFragment do this job for portrait orientation, so return
        if (btnTorStart == null) {
            return;
        }

        presenter = new TorFragmentPresenter(this);

        receiver = new TorFragmentReceiver(this, presenter);

        Context context = getActivity();
        if (context != null) {
            IntentFilter intentFilterBckgIntSer = new IntentFilter(RootExecService.COMMAND_RESULT);
            IntentFilter intentFilterTopFrg = new IntentFilter(TopFragment.TOP_BROADCAST);

            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilterBckgIntSer);
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilterTopFrg);

            presenter.onStart();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (TopFragment.logsTextSize != 0f) {
            setLogsTextSize(TopFragment.logsTextSize);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        try {
            Context context = getActivity();
            if (context != null && receiver != null) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
            }
        } catch (Exception e) {
            loge("TorFragment onStop", e);
        }

        if (presenter != null) {
            presenter.onStop();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (svTorLog != null) {
            svTorLog.setOnTouchListener(null);

            ViewTreeObserver observer = svTorLog.getViewTreeObserver();
            if (observer != null) {
                observer.removeOnScrollChangedListener(this);
            }
        }

        btnTorStart = null;
        tvTorStatus = null;
        pbTor = null;
        divTor = null;
        tvTorLog = null;
        svTorLog = null;

        receiver = null;

        presenter = null;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnTorStart) {
            letFirewallStop();
            presenter.startButtonOnClick();
        }
    }

    private void letFirewallStop() {
        ModulesStatus modulesStatus = ModulesStatus.getInstance();
        if (modulesStatus.getFirewallState() != STOPPED
                && modulesStatus.getTorState() == RUNNING
                && (modulesStatus.getDnsCryptState() == STOPPED
                || modulesStatus.getDnsCryptState() == STOPPING
                || modulesStatus.getDnsCryptState() == FAULT)
                && (modulesStatus.getItpdState() == STOPPED
                || modulesStatus.getItpdState() == STOPPING
                || modulesStatus.getItpdState() == FAULT)) {
            modulesStatus.setFirewallState(
                    STOPPING,
                    App.getInstance().getDaggerComponent().getPreferenceRepository().get()
            );
        }
    }

    @Override
    public void setTorStatus(int resourceText, int resourceColor) {
        tvTorStatus.setText(resourceText);
        tvTorStatus.setTextColor(getResources().getColor(resourceColor));
    }

    @Override
    public void setTorStatus(String text, int resourceColor) {
        tvTorStatus.setText(text);
        tvTorStatus.setTextColor(getResources().getColor(resourceColor));
    }

    @Override
    public void setTorStartButtonEnabled(boolean enabled) {
        if (btnTorStart.isEnabled() && !enabled) {
            btnTorStart.setEnabled(false);
        } else if (!btnTorStart.isEnabled() && enabled) {
            btnTorStart.setEnabled(true);
        }
    }

    @Override
    public void setStartButtonText(int textId) {
        btnTorStart.setText(textId);
    }

    @Override
    public void setTorProgressBarIndeterminate(boolean indeterminate) {
        if (indeterminate) {
            pbTor.setIndeterminate(true);
            pbTor.setVisibility(View.VISIBLE);
            divTor.setVisibility(View.GONE);
        } else {
            pbTor.setIndeterminate(false);
            pbTor.setVisibility(View.GONE);
            divTor.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setTorProgressBarProgress(int progress) {
        if (pbTor.isIndeterminate()) {
            pbTor.setIndeterminate(false);
        }
        if (progress >= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pbTor.setProgress(progress, true);
            } else {
                pbTor.setProgress(progress);
            }
            pbTor.setVisibility(View.VISIBLE);
            divTor.setVisibility(View.GONE);
        } else {
            pbTor.setVisibility(View.GONE);
            divTor.setVisibility(View.VISIBLE);
        }
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void setTorLogViewText() {
        tvTorLog.setText(getText(R.string.tvTorDefaultLog) + " " + TorVersion);
    }

    @Override
    public void setTorLogViewText(Spanned text) {
        tvTorLog.setText(text);
    }

    @Override
    public void setLogsTextSize(float size) {
        if (tvTorLog != null) {
            tvTorLog.setTextSize(COMPLEX_UNIT_PX, size);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (presenter != null && motionEvent.getPointerCount() == 2) {
            ScaleGestureDetector detector = presenter.getScaleGestureDetector();
            if (detector != null) {
                detector.onTouchEvent(motionEvent);
                return true;
            }
        }
        return false;
    }

    @Override
    public Activity getFragmentActivity() {
        return getActivity();
    }

    @Override
    public FragmentManager getFragmentFragmentManager() {
        return getParentFragmentManager();
    }

    public TorFragmentPresenterInterface getPresenter() {
        Activity activity = getActivity();
        if (presenter == null && activity instanceof MainActivity
                && ((MainActivity) activity).getMainFragment() != null) {
            presenter = ((MainActivity) activity).getMainFragment().getTorFragmentPresenter();
        }

        return presenter;
    }

    @Override
    public void onScrollChanged() {
        if (presenter != null && svTorLog != null) {
            presenter.torLogAutoScrollingAllowed(!svTorLog.canScrollVertically(1)
                    || !svTorLog.canScrollVertically(-1));
        }
    }

    @Override
    public void scrollTorLogViewToBottom() {
        if (svTorLog == null) {
            return;
        }

        svTorLog.post(() -> {
            if (svTorLog == null) {
                return;
            }

            svTorLog.computeScroll();

            int delta = 0;

            int childIndex = svTorLog.getChildCount() - 1;

            if (childIndex < 0) {
                return;
            }

            View lastChild = svTorLog.getChildAt(childIndex);

            if (lastChild != null) {
                int bottom = lastChild.getBottom() + svTorLog.getPaddingBottom();
                int sy = svTorLog.getScrollY();
                int sh = svTorLog.getHeight();
                delta = bottom - (sy + sh);
            }

            if (delta > 0) {
                svTorLog.smoothScrollBy(0, delta);
            }
        });
    }
}
