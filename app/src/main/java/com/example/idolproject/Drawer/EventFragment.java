package com.example.idolproject.Drawer;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.idolproject.R;

public class EventFragment extends Fragment {

    public EventFragment() {
        super(R.layout.fragment_event);  // XML 파일 이름에 맞게 수정
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 초기화 작업 여기서
    }
}
