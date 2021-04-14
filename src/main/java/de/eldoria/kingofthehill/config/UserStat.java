package de.eldoria.kingofthehill.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class UserStat implements Comparable<UserStat> {
    private long id;
    private double points = 0;

    public UserStat(long id) {
        this.id = id;
    }

    public void upcount() {
        points++;
    }

    public void upcount(double value) {
        points += value;
    }

    @Deprecated
    public void reduce() {
        points *= 0.999;
    }

    @Override
    public int compareTo(@NotNull UserStat o) {
        return Double.compare(points, o.points);
    }

    public void reduce(int points) {
        this.points -= points;
    }

    public double draw() {
        double v = points * 0.0004;
        points -= v;
        return v;
    }

    public void forcePercent(double total) {
        if (points == 0.0) return;
        points = (points / total) * 1000;
    }

    public double getPercent() {
        return points / 1000 * 100;
    }
}
