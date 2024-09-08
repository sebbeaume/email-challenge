# Mailtime!

To ensure workers are making the most of their time, HQ has decided to track the time it takes for employees to reply on
emails...

You will be presented an extract of the emails sent in the company, between workers at different locations around the
world.
Your goal is to find the average response time in seconds, rounded up, of each of these workers.

### Example:

- Alice sent an email the Bob on the **Friday 12 January at 15:00 Paris time**, which corresponds to **21:00 in
  Singapore Time**
- Bob responded to this email on the **Monday 15 January at 9:00 Singapore time**, which corresponds to **03:00 in Paris
  time**
- Alice responded to this email on the *Tuesday 16 january at 09:05 Paris time**
- The time elapsed between the moment Bob received the email and the time he answered is **60 hours**, or **216 000
  seconds**
- The time elapsed between the moment Alice received Bob's response and the time she answered is **30 hours and 5
  minutes**, or **108 300 seconds**

The company is however aware that simply checking the average response time is not a significant KPI, and could lead to
misinterpretation of the employees performance, as they are based in different timezones. Thus, you will be rewarded
only a **quarter of the maximum possible points** for finding these statistic correctly.

For full grade, the company wants you to take into account the working hours of each of the employees when calculating
their average response time.

Example:

- Alice sent an email the Bob on the **Friday 12 January at 15:00 Paris time**, which corresponds to **21:00 in
  Singapore Time**
- Bob responded to this email on the **Monday 15 January at 9:00 Singapore time**, which corresponds to **03:00 in Paris
  time**
- Alice responded to this email on the *Tuesday 16 january at 09:05 Paris time**
- Bob, like every employee of the company, **doesn't work on the weekends**
- Bob answered **1 hour after the start of his next working day** : he thus took **3600 seconds** to answer the email
- Alice received the response on the Monday morning at 3Am **before her working hours**
- Alice failed to respond during her working hours of **9 AM to 6 PM**
- Alice responded on the next day, **5 minutes after the start of her working day**
- Thus, she took **one full working day + 5 minutes** to answer, or (18-9) hours + 5 minutes = **32 700 seconds**

## Your input

```json
{
  "emails": [
    {
      "subject": "subject",
      "sender": "Alice",
      "receiver": "Bob",
      "timeSent": "2024-01-12T15:00:00+01:00"
    },
    {
      "subject": "RE: subject",
      "sender": "Bob",
      "receiver": "Alice",
      "timeSent": "2024-01-15T09:00:00+08:00"
    },
    {
      "subject": "RE: RE: subject",
      "sender": "Alice",
      "receiver": "Bob",
      "timeSent": "2024-01-16T9:05:00+01:00"
    }
  ],
  "users": [
    {
      "name": "Alice",
      "officeHours": {
        "timeZone": "Europe/Paris",
        "start": 9,
        "end": 18
      }
    },
    {
      "name": "Bob",
      "officeHours": {
        "timeZone": "Asia/Singapore",
        "start": 8,
        "end": 17
      }
    }
  ]
}
```

## Your expected output

```json
{
  "Alice": 32700,
  "Bob": 3600
}
```