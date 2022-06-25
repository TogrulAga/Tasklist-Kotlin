package tasklist

import java.util.Scanner
import kotlin.system.exitProcess
import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File


const val LINE_CAPACITY = 44

fun main() {
    TaskList.run()
}


object TaskList {
    private val validActions = listOf("add", "print", "edit", "delete", "end")
    private val tasks = mutableListOf<Task>()
    private val emptyLinePattern = """^\s+$""".toRegex()
    private val scanner = Scanner(System.`in`)
    private val datePattern = """(\d{4})-(\d{1,2})-(\d{1,2})""".toRegex()
    private val timePattern = """(\d{1,2}):(\d{1,2})""".toRegex()
    private lateinit var taskListAdapter: JsonAdapter<List<Task?>>

    fun run() {
        initMoshi()
        loadTasks()

        while (true) {
            when (getAction()) {
                "add" -> addTask()
                "print" -> printTasks()
                "edit" -> editTask()
                "delete" -> deleteTask()
                "end" -> exit()
            }
        }
    }

    private fun initMoshi() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)

        taskListAdapter = moshi.adapter(type)
    }

    private fun loadTasks() {
        val jsonFile = File("tasklist.json")
        if (!jsonFile.exists()) {
            return
        }

        val jsonContent = jsonFile.readText()

        val loadedTasks = taskListAdapter.fromJson(jsonContent)

        for (task in loadedTasks!!)
        {
            tasks.add(task!!)
        }
    }

    private fun saveTasks() {
        val jsonFile = File("tasklist.json")

        jsonFile.createNewFile()
        val jsonContent = taskListAdapter.toJson(tasks)

        jsonFile.writeText(jsonContent)
    }

    private fun deleteTask() {
        if (tasks.isEmpty()) {
            println("No tasks have been input")
            return
        }

        printTasks()

        val taskNumber = getTaskNumber()

        tasks.removeAt(taskNumber)
        println("The task is deleted")
    }

    private fun editTask() {
        if (tasks.isEmpty()) {
            println("No tasks have been input")
            return
        }

        printTasks()

        val taskNumber = getTaskNumber()

        when (getTaskField()) {
            "priority" -> tasks[taskNumber].setPriority(getPriority())
            "date" -> tasks[taskNumber].setDate(getDate())
            "time" -> tasks[taskNumber].setTime(getTime())
            "task" -> tasks[taskNumber].setTask(getTask().toMutableList())
        }


        println("The task is changed")
    }

    private fun getTaskField(): String {
        val validFields = listOf("priority", "date", "time", "task")
        var field: String
        while (true) {
            println("Input a field to edit (priority, date, time, task):")
            field = readln()

            if (field !in validFields) {
                println("Invalid field")
            } else {
                return field
            }
        }
    }

    private fun getTaskNumber(): Int {
        var taskNumber: Int
        while (true) {
            println("Input the task number (1-${tasks.size}):")
            val line = readln()

            try {
                taskNumber = line.toInt()

                if (taskNumber > tasks.size || taskNumber < 1) {
                    throw NumberFormatException()
                } else {
                    break
                }
            } catch (e: NumberFormatException) {
                println("Invalid task number")
            }
        }

        return taskNumber - 1
    }

    private fun getAction(): String {
        while (true) {
            println("Input an action (add, print, edit, delete, end):")
            val action = readln()

            if (action in validActions) {
                return action
            }

            println("The input action is invalid")
        }
    }

    private fun printTasks() {
        if (tasks.isEmpty()) {
            println("No tasks have been input")
            return
        }

        val lineSep = "+----+------------+-------+---+---+--------------------------------------------+\n"
        val header = "| N  |    Date    | Time  | P | D |                   Task                     |\n"
        val table = StringBuilder()

        table.append(lineSep)
        table.append(header)
        table.append(lineSep)

        for ((index, task) in tasks.withIndex()) {
            table.append(task.toStringRepr(index + 1))
            table.append(lineSep)
        }

        println(table.toString())
    }

    private fun addTask() {
        val priority = getPriority()
        val date = getDate()
        val time = getTime()
        val taskContent = getTask()

        val task = Task(content = taskContent.toMutableList(), priority = priority)
        task.setDate(date)
        task.setTime(time)

        if (task.isEmpty()) {
            println("The task is blank")
            return
        }

        tasks.add(task)
    }

    private fun getTask(): List<String> {
        val task = mutableListOf<String>()
        println("Input a new task (enter a blank line to end):")

        while (true) {
            val line = scanner.nextLine().trimEnd().trimStart()
            if (line.isEmpty() || emptyLinePattern.matches(line)) {
                break
            }
            task.add(line)
        }

        return task
    }

    private fun getTime(): LocalDateTime {
        var time: LocalDateTime
        while (true) {
            println("Input the time (hh:mm):")
            val line = readln()
            val match = timePattern.find(line)

            if (match != null) {
                val hour = match.groups[1]?.value
                val minute = match.groups[2]?.value

                try {
                    if (hour != null && minute != null) {
                        time = LocalDateTime(year = 2000, monthNumber = 1, dayOfMonth = 1, hour = hour.toInt(), minute = minute.toInt())
                        break
                    }

                    throw IllegalArgumentException()
                } catch (e: IllegalArgumentException) {
                    println("The input time is invalid")
                }
            } else {
                println("The input time is invalid")
            }
        }

        return time
    }

    private fun getDate(): LocalDate {
        var date: LocalDate
        while (true) {
            println("Input the date (yyyy-mm-dd):")
            val line = readln()
            val match = datePattern.find(line)

            if (match != null) {
                val year = match.groups[1]?.value
                val month = match.groups[2]?.value
                val day = match.groups[3]?.value

                try {
                    if (year != null && month != null && day != null) {
                        date = LocalDate(year = year.toInt(), monthNumber = month.toInt(), dayOfMonth = day.toInt())
                        break
                    }
                    throw IllegalStateException()
                } catch (e: IllegalArgumentException) {
                    println("The input date is invalid")
                }
            } else {
                println("The input date is invalid")
            }
        }

        return date
    }


    private fun getPriority(): Priority {
        var priority: Priority
        while (true) {
            println("Input the task priority (C, H, N, L):")
            val line = readln().uppercase()

            try {
                priority = Priority.valueOf(line)
                break
            } catch (e: IllegalArgumentException) {
                continue
            }
        }

        return priority
    }

    private fun exit() {
        println("Tasklist exiting!")
        saveTasks()
        exitProcess(0)
    }

}


class Task(private var content: MutableList<String> = mutableListOf(), private var priority: Priority) {
    private var year: Int = 0
    private var month: Int = 0
    private var day: Int = 0
    private var hour: Int = 0
    private var minute: Int = 0

    private fun getDueTag(): DueTag {
        val dateTime = LocalDateTime(year, month, day, hour, minute)

        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+2")).date
        val numberOfDays = currentDate.daysUntil(dateTime.date)

        val dueTag: DueTag = if (numberOfDays == 0) {
            DueTag.T
        } else if (numberOfDays > 0) {
            DueTag.I
        } else {
            DueTag.O
        }

        return dueTag
    }

    fun toStringRepr(index: Int): String {
        val dateTime = LocalDateTime(year, month, day, hour, minute)

        val indent = if (index.toString().length == 1) "  " else " "
        val result = StringBuilder()

        val hourString = if (dateTime.hour < 10) "0" + dateTime.hour.toString() else dateTime.hour.toString()
        val minuteString = if (dateTime.minute < 10) "0" + dateTime.minute.toString() else dateTime.minute.toString()

        for ((i, line) in content.withIndex()) {
            if (i == 0) {
                if (line.length > LINE_CAPACITY) {
                    for ((j, l) in line.chunked(LINE_CAPACITY).withIndex()) {
                        val paddingLength = 44 - l.length
                        if (j == 0) {
                            result.append("| $index$indent| ${dateTime.date} | $hourString:$minuteString | ${priority.coloredRepr} | ${getDueTag().coloredRepr} |$l${" ".repeat(paddingLength)}|\n")
                        } else {
                            result.append("|    |            |       |   |   |$l${" ".repeat(paddingLength)}|\n")
                        }
                    }
                } else {
                    val paddingLength = 44 - line.length
                    result.append("| $index$indent| ${dateTime.date} | $hourString:$minuteString | ${priority.coloredRepr} | ${getDueTag().coloredRepr} |$line${" ".repeat(paddingLength)}|\n")
                }
            } else {
                if (line.length > LINE_CAPACITY) {
                    for (l in line.chunked(LINE_CAPACITY)) {
                        val paddingLength = 44 - l.length
                        result.append("|    |            |       |   |   |$l${" ".repeat(paddingLength)}|\n")
                    }
                } else {
                    val paddingLength = 44 - line.length
                    result.append("|    |            |       |   |   |$line${" ".repeat(paddingLength)}|\n")
                }
            }
        }

        return result.toString()
    }

    fun isEmpty(): Boolean {
        return content.isEmpty()
    }

    fun setPriority(priority: Priority) {
        this.priority = priority
    }

    fun setDate(date: LocalDate) {
        year = date.year
        month = date.monthNumber
        day = date.dayOfMonth
    }

    fun setTime(time: LocalDateTime) {
        hour = time.hour
        minute = time.minute
    }

    fun setTask(task: MutableList<String>) {
        content = task
    }
}

enum class Priority(val string: String, val coloredRepr: String) {
    C("Critical", "\u001B[101m \u001B[0m"),
    H("High", "\u001B[103m \u001B[0m"),
    N("Normal", "\u001B[102m \u001B[0m"),
    L("Low", "\u001B[104m \u001B[0m")
}

enum class DueTag(val string: String, val coloredRepr: String) {
    I("In time", "\u001B[102m \u001B[0m"),
    T("Today", "\u001B[103m \u001B[0m"),
    O("Overdue", "\u001B[101m \u001B[0m")
}