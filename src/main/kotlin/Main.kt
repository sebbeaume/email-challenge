package org.example

import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val emails = generateInput();
    println(emails)
}

fun generateInput(): List<Email> {
    val officeHours = listOf(
        OfficeHours(ZoneId.of("Europe/Paris"), 9, 18),
        OfficeHours(ZoneId.of("Australia/Sydney"), 10, 18),
        OfficeHours(ZoneId.of("Asia/Singapore"), 8, 17),
        OfficeHours(ZoneId.of("Hongkong"), 8, 17),
        OfficeHours(ZoneId.of("America/New_York"), 10, 18),
        OfficeHours(ZoneId.of("America/Los_Angeles"), 7, 16)
    );

    val users = arrayListOf<User>();
    for(i in 0..9) {
        users.add(User(getRandomString(5), officeHours.random()))
    }
    val emailLoops = arrayListOf<List<Email>>();
    for(i in 0..Random().nextInt(10,15)) {
        emailLoops.add(generateEmailLoop(users.shuffled().take(Random().nextInt(2, 5))));
    }

return emailLoops.flatten().shuffled();
}

fun generateEmailLoop(users:List<User> ): List<Email> {
    val loop = arrayListOf<Email>();
    val sender = users[0];
    val subject = getRandomString(10);
    val receiver = users[1];
    val sentTime = ZonedDateTime.of(2024,1, 8,(sender.officeHours.start..<sender.officeHours.end).random(),
        (0..59).random(), (0..59).random(), 0, sender.officeHours.timeZone);
    var email = Email(subject, sender, receiver, sentTime)
    loop.add(email);
    for(i in 0..(5..10).random()) {
        val response = generateResponse(email, users);
        loop.add(response);
        email = response;
    }
    return loop;
}

fun generateResponse(email: Email, users: List<User>):Email {
    val subject = "RE: "+email.subject;
    val sender = email.receiver;
    val receiver = users.filter { user -> user.name != sender.name }.random();
    val responseTime = generateResponseTime(email.timeSent, sender);
    return Email(subject, sender, receiver, responseTime);
}

fun generateResponseTime(timeReceived: ZonedDateTime, user:User):ZonedDateTime {
    var timeResponded = timeReceived.withZoneSameInstant(user.officeHours.timeZone);
    timeResponded = timeResponded
        .withHour((user.officeHours.start..<user.officeHours.end).random())
        .withMinute((0..59).random())
        .withSecond((0..59).random());
    if(timeResponded.isBefore(timeReceived)) {
        timeResponded = timeResponded.plusDays(1);
    }
    // Add a 25% chance that the next working day is also skipped
    if(Random().nextInt(0,100) < 25) {
        timeResponded = timeResponded.plusDays(1);

    }
    while(timeResponded.dayOfWeek == DayOfWeek.SATURDAY || timeResponded.dayOfWeek == DayOfWeek.SUNDAY) {
        timeResponded = timeResponded.plusDays(1);
    }
    val responseTime = calculateResponseTime(timeReceived, timeResponded, user);
    user.responseTimes = arrayListOf(*user.responseTimes.toTypedArray(), responseTime);
    user.responseTimesWithoutOfficeHours = arrayListOf(*user.responseTimesWithoutOfficeHours.toTypedArray(), ChronoUnit.SECONDS.between(timeResponded, timeReceived))

    return timeResponded;
}

fun calculateResponseTime(timeReceived: ZonedDateTime, timeResponded: ZonedDateTime, user:User): Long {
    if(timeReceived.withZoneSameInstant(user.officeHours.timeZone).dayOfYear == timeResponded.dayOfYear){
        //Sent on the same day: need to check if it was received before office hours
        if(timeReceived.withZoneSameInstant(user.officeHours.timeZone).hour < user.officeHours.start) {
            // Received before office hours : return difference between response time and start of office hours
            val startOfOfficeHours = timeResponded.withHour(user.officeHours.start).withMinute(0).withSecond(0);
            return ChronoUnit.SECONDS.between(startOfOfficeHours, timeResponded);
        }
        return ChronoUnit.SECONDS.between(timeReceived, timeResponded)
    }
    // Received on a previous day - need to check if there was a full working day in between
    val emailNotRespondedToDuringAFullWorkingDay = ((timeReceived.dayOfWeek == DayOfWeek.FRIDAY && timeResponded.dayOfWeek == DayOfWeek.TUESDAY) ||
        (timeReceived.dayOfWeek == DayOfWeek.THURSDAY && timeResponded.dayOfWeek == DayOfWeek.MONDAY) ||
        (timeResponded.dayOfWeek.value - timeReceived.dayOfWeek.value > 1));

    val timeTakenDuringFullDay = if (emailNotRespondedToDuringAFullWorkingDay) (user.officeHours.end - user.officeHours.start) * 3600 else 0;
    // Received on previous day - need to check if received before end of office hours
    if(timeReceived.withZoneSameInstant(user.officeHours.timeZone).hour < user.officeHours.end) {
        // Received during working hours : time for response is time Left in the working day + time taken during next working day
        val endOfOfficeDay = timeReceived.withHour(user.officeHours.end).withMinute(0).withSecond(0);
        val startOfOfficeHours = timeResponded.withHour(user.officeHours.start).withMinute(0).withSecond(0);
        return timeTakenDuringFullDay + ChronoUnit.SECONDS.between(timeReceived, endOfOfficeDay) + ChronoUnit.SECONDS.between(startOfOfficeHours, timeResponded)
    }
        val startOfOfficeHours = timeResponded.withHour(user.officeHours.start).withMinute(0).withSecond(0);
        return timeTakenDuringFullDay + ChronoUnit.SECONDS.between(startOfOfficeHours, timeResponded);

}

data class User(val name: String, val officeHours: OfficeHours,var responseTimesWithoutOfficeHours: List<Long> = arrayListOf(), var responseTimes: List<Long> = arrayListOf());

data class OfficeHours(val timeZone: ZoneId, val start: Int, val end: Int);

data class Email(val subject: String, val sender: User, val receiver: User, val timeSent: ZonedDateTime);


fun getRandomString(length: Int) : String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}
