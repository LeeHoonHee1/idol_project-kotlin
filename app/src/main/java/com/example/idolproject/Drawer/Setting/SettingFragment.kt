package com.example.idolproject.Drawer.Setting

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.example.idolproject.R
import androidx.compose.foundation.layout.ColumnScope

class SettingFragment : Fragment(R.layout.fragment_setting) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val composeView = view.findViewById<ComposeView>(R.id.compose_setting)

        composeView.setContent {
            MaterialTheme {
                SettingScreen(
                    onClickItem = { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingScreen(
    onClickItem: (String) -> Unit
) {
    var pushEnabled by remember { mutableStateOf(true) }
    var eventAlertEnabled by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAF7FF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "설정",
                color = Color(0xFF2D2440),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "앱 사용 환경과 정보를 확인할 수 있어요",
                color = Color(0xFF7E748E),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(18.dp))

            SettingSectionTitle(title = "앱 설정")

            SettingCard {
                SettingSwitchRow(
                    title = "푸시 알림",
                    description = "친구 요청, 채팅, 이벤트 알림을 받아요",
                    checked = pushEnabled,
                    onCheckedChange = { pushEnabled = it }
                )

                SettingDivider()

                SettingSwitchRow(
                    title = "이벤트 알림",
                    description = "진행 중인 팬 이벤트 소식을 받아요",
                    checked = eventAlertEnabled,
                    onCheckedChange = { eventAlertEnabled = it }
                )

                SettingDivider()

                SettingTextRow(
                    title = "테마 설정",
                    description = "현재는 라벤더 기본 테마를 사용 중이에요",
                    actionText = "보기",
                    onClick = {
                        onClickItem("테마 설정은 나중에 연결할게")
                    }
                )

                SettingDivider()

                SettingTextRow(
                    title = "캐시 관리",
                    description = "Room에 저장된 이벤트 캐시를 관리할 수 있어요",
                    actionText = "관리",
                    onClick = {
                        onClickItem("캐시 관리 기능은 나중에 연결할게")
                    }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            SettingSectionTitle(title = "정보")

            SettingCard {
                SettingInfoRow(
                    title = "앱 이름",
                    value = "IdolProject"
                )

                SettingDivider()

                SettingInfoRow(
                    title = "앱 버전",
                    value = "1.0.0"
                )

                SettingDivider()

                SettingTextRow(
                    title = "포트폴리오 기술 스택",
                    description = "MVVM, Hilt, Flow, DataStore, Unit Test, Retrofit, Room, Compose",
                    actionText = "확인",
                    onClick = {
                        onClickItem("포트폴리오 기술 스택을 적용 중이야")
                    }
                )

                SettingDivider()

                SettingInfoRow(
                    title = "화면 구성",
                    value = "Jetpack Compose"
                )
            }
        }
    }
}

@Composable
private fun SettingSectionTitle(
    title: String
) {
    Text(
        text = title,
        color = Color(0xFF2D2440),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SettingCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color(0xFF2D2440),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = Color(0xFF8A7CA0),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingTextRow(
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color(0xFF2D2440),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = Color(0xFF8A7CA0),
                style = MaterialTheme.typography.bodySmall
            )
        }

        TextButton(
            onClick = onClick
        ) {
            Text(text = actionText)
        }
    }
}

@Composable
private fun SettingInfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFF2D2440),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = Color(0xFF8A7CA0),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SettingDivider() {
    Divider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = Color(0xFFECE4FF)
    )
}