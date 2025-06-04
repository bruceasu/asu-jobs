# asu-jobs
This program is designed for Windows users to execute a cron job without requiring the permissions of the task scheduler user.

# Config

The config files locate at `$HOME.config\cron` folder.
- crontab.txt
- at.txt
- cron.properties

The content e.g.

crontab.txt
> 0 * * * * ? cmd /c echo hello  >> hello.txt
> 0 0/5 * * * ? cmd /c echo world  >> hello.txt


at.txt
> 2025-06-04T22:00 cmd /c echo hello world >> hello
> 2025-06-05T22:00 cmd /c echo hello world >> hello

cron.properties
> editor=C:\\green\\Notepad4\\notepad4.exe
> port=8080

# Usage
- asu-jobs.exe help
`Usage: start | stop | reload | list | edit | help | add | every | at [arguments]`

- asu-jobs.exe start
`start the crond. and open 8080 to listen reload / stop command`

- asu-jobs.exe reload
`reload the crontab.txt and at.txt files.`

- asu-jobs.exe stop
`stop the crond`

- asu-jobs.exe list
`print the the crontab.txt and at.txt files.`

- asu-jobs.exe add "CRON-EXPRESSION" "CMD"

- asu-jobs.exe every xxx "CMD"

  - every month on Mon:1 at HH:mm CMD
  - every month on Mon at HH:mm CMD
  - every month on Last at HH:mm CMD
  - every month on First at HH:mm CMD
  - every month on DD at HH:mm CMD
  - every day at HH:mm CMD
  - every weekend at HH:mm CMD
  - every Workday at HH:mm CMD
  - every (Mon|Mon,Wed|Mon-Thu) at HH:mm CMD
  - every N hours CMD
  - every hour CMD
  - every N minutes CMD
  - every minute CMD

- asu-jobs.exe at xxx "CMD"
  - at HH:mm on MM-dd CMD
  - at HH:mm on MMM dd CMD
  - at HH:mm on CMD
  - at HH:mm CMD

- asu-jobs.exe edit
`Use the text editor to modify the file named crontab.txt. The default editor is notepad.exe.`
