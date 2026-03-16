package com.example.scrollersdashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodoHabitPanel(db: AppDatabase, appName: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    
    val refreshDailyState by db.scrollDao().getSettingFlow("refresh_daily_todo").collectAsState(initial = "true")
    val isRefreshDaily = refreshDailyState?.toBoolean() ?: true

    // If Refresh Daily is OFF, we use a fixed date "permanent_todo" to store/retrieve them
    val todoDate = if (isRefreshDaily) today else "permanent_todo"
    
    val todoTasks by db.scrollDao().getTodoTasks(todoDate).collectAsState(initial = emptyList())
    val habitTasks by db.scrollDao().getHabitTasks().collectAsState(initial = emptyList())

    var newTaskTitle by remember { mutableStateOf("") }
    var showAddOverlay by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf("todo") }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clickable(enabled = false) {},
                cornerRadius = 32.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Daily Focus",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF9333EA).copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0xFF9333EA).copy(alpha = 0.3f))
                        ) {
                            Text(
                                "${appName.uppercase()} LIMIT REACHED",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color(0xFF9333EA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    PanelSection(title = "Today's Tasks", onAdd = {
                        dialogType = "todo"
                        showAddOverlay = true
                    }) {
                        if (todoTasks.isEmpty()) {
                            Text(
                                "No tasks for today. Stay focused!",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(todoTasks, key = { it.id }) { task ->
                                    PanelTaskRow(
                                        title = task.title,
                                        isCompleted = task.isCompleted,
                                        onToggle = {
                                            scope.launch { db.scrollDao().insertTodo(task.copy(isCompleted = !task.isCompleted)) }
                                        },
                                        onDelete = {
                                            scope.launch { db.scrollDao().deleteTodo(task.id) }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PanelSection(title = "Habits", onAdd = {
                        dialogType = "habit"
                        showAddOverlay = true
                    }) {
                        if (habitTasks.isEmpty()) {
                            Text(
                                "No habits tracked",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(habitTasks, key = { it.id }) { habit ->
                                    val isCompletedToday = habit.lastCompletedDate == today
                                    PanelTaskRow(
                                        title = habit.title,
                                        isCompleted = isCompletedToday,
                                        onToggle = {
                                            scope.launch {
                                                db.scrollDao().insertHabit(habit.copy(lastCompletedDate = if (isCompletedToday) "" else today))
                                            }
                                        },
                                        onDelete = {
                                            scope.launch { db.scrollDao().deleteHabit(habit.id) }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    PremiumScalingButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        isDarkMode = true,
                        cornerRadius = 20.dp
                    ) {
                        Text(
                            "Dismiss & Return", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 17.sp, 
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (showAddOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showAddOverlay = false }
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight().clickable(enabled = false) {},
                    cornerRadius = 24.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "New ${if (dialogType == "todo") "Task" else "Habit"}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showAddOverlay = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("Enter title...", color = Color.White.copy(alpha = 0.3f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF9333EA),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF9333EA)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    PremiumScalingButton(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                scope.launch {
                                    if (dialogType == "todo") {
                                        db.scrollDao().insertTodo(TodoTask(title = newTaskTitle, date = todoDate))
                                    } else {
                                        db.scrollDao().insertHabit(HabitTask(title = newTaskTitle))
                                    }
                                    newTaskTitle = ""
                                    showAddOverlay = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        isDarkMode = true,
                        cornerRadius = 16.dp
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PanelSection(title: String, onAdd: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title.uppercase(),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun PanelTaskRow(
    title: String,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        BorderStroke(
                            2.dp,
                            if (isCompleted) Color(0xFF9333EA) else Color.White.copy(alpha = 0.3f)
                        ),
                        CircleShape
                    )
                    .background(
                        if (isCompleted) Color(0xFF9333EA).copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF9333EA), modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = if (isCompleted) Color.White.copy(alpha = 0.4f) else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
