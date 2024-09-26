package com.example.photothis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photothis.databinding.ActivityTodoListBinding;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TodoListActivity extends AppCompatActivity implements IDateClickListener {
    private ActivityTodoListBinding binding;
    private LocalDate selectedDate;
    private DBManager dbManager;
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTodoListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // RecyclerView 초기화
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Adapter에 클릭 리스너를 추가
        taskAdapter = new TaskAdapter(new ArrayList<>(), this::showPopupDialog);
        recyclerView.setAdapter(taskAdapter);

        dbManager = new DBManager();
        dbManager.setTaskListObserver(this::updateTaskList); // Observer 설정

        // 초기 선택 날짜 설정
        selectedDate = LocalDate.now();
        dbManager.setSelectedDate(selectedDate); // 선택된 날짜 설정
        dbManager.loadTasks(); // 초기 작업 로딩

        // 날짜 표시 초기화
        updateSelectedDateDisplay(); // 현재 날짜로 상단 텍스트 업데이트

        binding.addButton.setOnClickListener(v -> showTaskDialog());

        // TaskDialogFragment와 PopupDialogFragment의 결과를 처리합니다.
        getSupportFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, result) -> {
            if (result.getBoolean("timeNotSelected", false)) {
                showTimeRequiredDialog();
                return;
            }

            String taskName = result.getString(TaskDialogFragment.KEY_TASK);
            String timeString = result.getString(TaskDialogFragment.KEY_TIME);
            boolean hasSpecificTime = timeString != null && !timeString.isEmpty();
            LocalTime taskTime = hasSpecificTime ? LocalTime.parse(timeString) : null;

            Task newTask = new Task("", taskName, taskTime, hasSpecificTime, 0, selectedDate);
            dbManager.saveTask(newTask.getTask(), newTask.getTime(), newTask.hasSpecificTime(), newTask.getDate());
        });

        getSupportFragmentManager().setFragmentResultListener("popupResult", this, (requestKey, result) -> {
            String taskId = result.getString("taskId"); // taskId를 String으로 가져옴
            boolean isDeleted = result.getBoolean("isDeleted", false); // 기본값 false


            if (isDeleted) {
                // 삭제 작업
                dbManager.deleteTask(taskId);
            } else {
                String taskName = result.getString("task");
                String timeString = result.getString("time");
                boolean hasSpecificTime = result.getBoolean("hasSpecificTime");
                LocalTime taskTime = timeString != null ? LocalTime.parse(timeString) : null;

                // Create an updated Task object
                Task updatedTask = new Task(taskId, taskName, taskTime, hasSpecificTime, 0, selectedDate);
                dbManager.updateTask(taskId, updatedTask); // Update using ID
            }
        });

        setOneWeekViewPager();

        // 메뉴 바 버튼 클릭 이벤트 설정
        Button calendarButton = findViewById(R.id.calendarButton);
        Button todoListButton = findViewById(R.id.todoListButton);
        Button myPageButton = findViewById(R.id.myPageButton);

        calendarButton.setOnClickListener(v -> {
            Intent intent = new Intent(TodoListActivity.this, MainActivity.class);
            startActivity(intent);
        });

        todoListButton.setOnClickListener(v -> {
            Intent intent = new Intent(TodoListActivity.this, TodoListActivity.class);
            startActivity(intent);
        });

        myPageButton.setOnClickListener(v -> {
            Intent intent = new Intent(TodoListActivity.this, MyPageActivity.class);
            startActivity(intent);
        });

    }

    private void showTaskDialog() {
        TaskDialogFragment dialog = new TaskDialogFragment();
        Bundle args = new Bundle();
        args.putString(TaskDialogFragment.KEY_DATE, selectedDate.toString());
        dialog.setArguments(args);

        dialog.show(getSupportFragmentManager(), "TaskDialog");
    }

    private void showTimeRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("시간 여부 확인")
                .setMessage("시간 여부를 선택해주세요.")
                .setPositiveButton("확인", null)
                .show();
    }

    private void showPopupDialog(Task task) {
        TaskDialog2Fragment dialog = new TaskDialog2Fragment();
        Bundle args = new Bundle();
        args.putString("task", task.getTask());
        args.putString("time", task.hasSpecificTime() ? task.getTime().toString() : "Anytime");
        args.putString("taskId", task.getId()); // Firebase ID 사용
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "PopupDialog");
    }

    private void updateTaskList(List<Task> newTaskList) {
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : newTaskList) {
            if (task.getDate().equals(selectedDate)) {
                filteredTasks.add(task);
            }
        }

        // 시간 순서로 정렬
        Collections.sort(filteredTasks, (t1, t2) -> {
            if (t1.getTime() == null && t2.getTime() == null) {
                return 0;
            } else if (t1.getTime() == null) {
                return 1;
            } else if (t2.getTime() == null) {
                return -1;
            } else {
                return t1.getTime().compareTo(t2.getTime());
            }
        });

        // 작업 번호를 날짜별로 초기화
        for (int i = 0; i < filteredTasks.size(); i++) {
            filteredTasks.get(i).setNumber(i + 1);
        }

        // RecyclerView를 업데이트합니다.
        taskAdapter.updateTasks(filteredTasks);
    }

    @Override
    protected void onStart() {
        super.onStart();
        dbManager.setSelectedDate(selectedDate); // 선택된 날짜를 DBManager에 설정
        dbManager.loadTasks(); // 선택된 날짜의 작업 목록 새로고침
        updateSelectedDateDisplay();
    }

    private void setOneWeekViewPager() {
        CalendarVPAdapter calendarAdapter = new CalendarVPAdapter(this, this, LocalDate.now());
        binding.mainWeeklyCalendarDateVp.setAdapter(calendarAdapter);
        binding.mainWeeklyCalendarDateVp.setCurrentItem(Integer.MAX_VALUE / 2, false); // 현재 주 중앙으로 설정
    }

    private void updateSelectedDateDisplay() {
        if (selectedDate != null) {
            String formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월"));
            binding.mainTopDateTv.setText(formattedDate);
        } else {
            binding.mainTopDateTv.setText("날짜 선택 안 됨");
        }
    }

    private void saveSelectedDate(LocalDate date) {
        SharedPreferences sharedPreference = getSharedPreferences("CALENDAR-APP", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreference.edit();
        editor.putString("SELECTED-DATE", date.toString());
        editor.apply();
    }

    @Override
    public void onClickDate(LocalDate date) {
        selectedDate = date;
        updateSelectedDateDisplay();
        dbManager.setSelectedDate(date); // DBManager에 선택된 날짜 설정
        dbManager.loadTasks(); // 선택된 날짜의 작업 목록 새로고침
    }
}
