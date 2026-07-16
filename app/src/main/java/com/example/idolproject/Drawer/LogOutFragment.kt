package com.example.idolproject.Drawer;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.idolproject.Login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class LogOutFragment extends Fragment {

    public LogOutFragment() {
        super(); // 레이아웃 없어도 됨
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.util.Log.d("LogOutFragment", "onCreate called");
        android.widget.Toast.makeText(requireContext(), "로그아웃 실행", android.widget.Toast.LENGTH_SHORT).show();

        clearFcmTokenAndLogout();
    }

    private void clearFcmTokenAndLogout() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            moveToLogin();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", FieldValue.delete())
                .addOnCompleteListener(task -> {
                    auth.signOut();
                    moveToLogin();
                });
    }

    private void moveToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // onCreate에서 이미 이동하니까 여기서는 할 일 없음
    }
}