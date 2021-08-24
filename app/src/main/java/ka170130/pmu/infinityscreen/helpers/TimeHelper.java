package ka170130.pmu.infinityscreen.helpers;

public class TimeHelper {

    public static String format(long millis) {
        // Calculate
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        String time = "";

        // Format hours
        if (hours > 0) {
            time += String.format("%d:", hours);
        }

        // Format minutes
        time += String.format("%02d:", minutes);

        // Format seconds
        time += String.format("%02d", seconds);

        return time;
    }

}
