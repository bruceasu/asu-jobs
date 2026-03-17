package me.asu.jobs;

import org.junit.Test;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CrontabTest {

    @Test
    public void shouldParseAbsoluteDateWithInlineCommand() {
        String at = Crontab.parseAt("at 14:30 2026-03-15 cmd /c echo hello");
        assertEquals("2026-03-15T14:30:00 cmd /c echo hello", at);
    }

    @Test
    public void shouldParseAbsoluteDateWithOptionalOnKeyword() {
        String at = Crontab.parseAt("at 14:30 on 2026-03-15 cmd /c echo hello");
        assertEquals("2026-03-15T14:30:00 cmd /c echo hello", at);
    }

    @Test
    public void shouldReadCommandFromStdInWhenMissingInlineCommand() throws Exception {
        String at = Crontab.buildAtEntry(Arrays.asList("at", "14:30"), new StringReader("cmd /c echo hello"),
                LocalDateTime.of(2026, 3, 14, 15, 0));
        assertEquals("2026-03-15T14:30:00 cmd /c echo hello", at);
    }

    @Test
    public void shouldSupportNamedTime() throws Exception {
        String at = Crontab.buildAtEntry(Arrays.asList("at", "teatime", "cmd", "/c", "echo", "tea"),
                new StringReader(""), LocalDateTime.of(2026, 3, 14, 10, 0));
        assertEquals("2026-03-14T16:00:00 cmd /c echo tea", at);
    }

    @Test
    public void shouldSupportRelativeTime() throws Exception {
        String at = Crontab.buildAtEntry(Arrays.asList("at", "now", "+", "30", "minutes", "cmd", "/c", "echo", "later"),
                new StringReader(""), LocalDateTime.of(2026, 3, 14, 10, 15));
        assertEquals("2026-03-14T10:45:00 cmd /c echo later", at);
    }

    @Test
    public void shouldKeepLegacyWeekdaySyntaxWorking() throws Exception {
        String at = Crontab.buildAtEntry(Arrays.asList("at", "14:30", "on", "Mon-Thu", "cmd", "/c", "echo", "legacy"),
                new StringReader(""), LocalDateTime.of(2026, 3, 15, 10, 0));
        assertEquals("2026-03-16T14:30:00 cmd /c echo legacy", at);
    }

    @Test
    public void shouldParseMayMonthName() throws Exception {
        String at = Crontab.buildAtEntry(Arrays.asList("at", "08:00", "on", "May", "6", "cmd", "/c", "echo", "may"),
                new StringReader(""), LocalDateTime.of(2026, 3, 15, 10, 0));
        assertEquals("2026-05-06T08:00:00 cmd /c echo may", at);
    }

    @Test
    public void shouldSupportAmPmTime() {
        String at = Crontab.parseAt("at 10:30pm 2026-03-15 cmd /c echo hello");
        assertEquals("2026-03-15T22:30:00 cmd /c echo hello", at);
    }

    @Test
    public void shouldSupportAmPmWithoutMinutes() throws Exception {
        String at = Crontab.buildAtEntry(Arrays.asList("at", "10am", "cmd", "/c", "echo", "hello"),
                new StringReader(""), LocalDateTime.of(2026, 3, 18, 9, 0));
        assertEquals("2026-03-18T10:00:00 cmd /c echo hello", at);
    }

    @Test
    public void shouldSupportAmPmWithOptionalOnKeyword() {
        String at = Crontab.parseAt("at 10pm on 2026-03-15 cmd /c echo hello");
        assertEquals("2026-03-15T22:00:00 cmd /c echo hello", at);
    }
}
