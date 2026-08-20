import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# 1. Change the type of selectedDayForAttendanceSheet
content = content.replace(
    'var selectedDayForAttendanceSheet by remember { mutableStateOf<Int?>(null) }',
    'var selectedDayForAttendanceSheet by remember { mutableStateOf<Pair<Int, com.example.domain.model.AttendanceStatus?>?>(null) }'
)

# 2. Update the references to selectedDayForAttendanceSheet when opening the sheet
# The caller in items() inside LazyColumn:
old_caller = """                    onOpenAttendanceSheet = { day ->
                        selectedDayForAttendanceSheet = day
                    }"""
new_caller = """                    onOpenAttendanceSheet = { day, initialStatus ->
                        selectedDayForAttendanceSheet = Pair(day, initialStatus)
                    }"""
content = content.replace(old_caller, new_caller)

# 3. Update the Bottom Sheet section
old_sheet = """    selectedDayForAttendanceSheet?.let { day ->
        val dateKey = LaborCalendarHelper.getDateKey(currentYear, currentMonthNum, day)
        val dayRecord = worker.attendance[dateKey]
        var currentStatus by remember(day, dayRecord) { mutableStateOf(dayRecord?.status ?: AttendanceStatus.UNMARKED) }"""

new_sheet = """    selectedDayForAttendanceSheet?.let { (day, initialStatus) ->
        val dateKey = LaborCalendarHelper.getDateKey(currentYear, currentMonthNum, day)
        val dayRecord = worker.attendance[dateKey]
        var currentStatus by remember(day, dayRecord, initialStatus) { 
            mutableStateOf(initialStatus ?: dayRecord?.status ?: AttendanceStatus.UNMARKED) 
        }"""
content = content.replace(old_sheet, new_sheet)

# 4. Update the LaborAttendanceDayRow function signature
old_sig = """    onAdvanceClicked: (Int) -> Unit,
    onOpenAttendanceSheet: (Int) -> Unit
) {"""
new_sig = """    onAdvanceClicked: (Int) -> Unit,
    onOpenAttendanceSheet: (Int, com.example.domain.model.AttendanceStatus?) -> Unit
) {"""
content = content.replace(old_sig, new_sig)

# 5. Update existing onOpenAttendanceSheet calls in LaborAttendanceDayRow
# AttendanceStatus.ABSENT ->
content = content.replace(
    'onClick = { onOpenAttendanceSheet(dayInfo.day) }',
    'onClick = { onOpenAttendanceSheet(dayInfo.day, null) }'
)

# 6. Update the 'else' branch where clicking A or P instantly saved
old_else_a = """                                    AttendancePillButton(
                                        label = "A",
                                        isSelected = false,
                                        activeColor = Color(0xFFEF4444),
                                        onClick = { onStatusSelected(dayInfo.day, AttendanceStatus.ABSENT) }
                                    )"""
new_else_a = """                                    AttendancePillButton(
                                        label = "A",
                                        isSelected = false,
                                        activeColor = Color(0xFFEF4444),
                                        onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.ABSENT) }
                                    )"""
content = content.replace(old_else_a, new_else_a)

old_else_p = """                                    AttendancePillButton(
                                        label = "P",
                                        isSelected = false,
                                        activeColor = Color(0xFF10B981),
                                        onClick = { onStatusSelected(dayInfo.day, AttendanceStatus.PRESENT) }
                                    )"""
new_else_p = """                                    AttendancePillButton(
                                        label = "P",
                                        isSelected = false,
                                        activeColor = Color(0xFF10B981),
                                        onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.PRESENT) }
                                    )"""
content = content.replace(old_else_p, new_else_p)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated LaborDetailScreen.kt")
