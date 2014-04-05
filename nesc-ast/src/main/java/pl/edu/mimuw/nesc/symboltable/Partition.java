package pl.edu.mimuw.nesc.symboltable;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Partition {

    private final String name;
    private Location visibleFrom;

    public Partition(String name) {
        this(name, null);
    }

    public Partition(String name, Location visibleFrom) {
        this.name = name;
        this.visibleFrom = visibleFrom;
    }

    public String getName() {
        return name;
    }

    public Location getVisibleFrom() {
        return visibleFrom;
    }
}
