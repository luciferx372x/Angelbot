package de.angelbot.core.bot.walk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Sucht einen Fußweg zwischen zwei Blöcken (A*).
 *
 * <p>Erlaubt sind Schritte in die vier Himmelsrichtungen: geradeaus, einen Block hinauf (Sprung) und
 * bis zu {@link #MAX_DROP} Blöcke hinunter. Diagonale Schritte, Schwimmen, Klettern und Springen über
 * Lücken gibt es bewusst nicht, damit der Weg einfach und vorhersagbar bleibt. Jede Zelle des Wegs
 * muss im erlaubten {@link Area Bereich} liegen.
 */
public final class PathFinder {

  /** Tiefster Abstieg in Blöcken. Bis 3 Blöcke gibt es noch keinen Fallschaden, wir nehmen 2. */
  public static final int MAX_DROP = 2;

  private static final int[] DX = {1, -1, 0, 0};
  private static final int[] DZ = {0, 0, 1, -1};

  private PathFinder() {}

  private static final class Node implements Comparable<Node> {
    final int x;
    final int y;
    final int z;
    double cost;
    double estimate;
    Node parent;
    boolean closed;

    Node(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @Override
    public int compareTo(Node other) {
      return Double.compare(this.estimate, other.estimate);
    }
  }

  /**
   * Sucht den Weg.
   *
   * @param maxNodes so viele Zellen werden höchstens untersucht (begrenzt die Rechenzeit)
   * @return die Zellen vom Start bis zum Ziel (beide eingeschlossen) als {@code {x, y, z}}, oder
   *     {@code null}, wenn es keinen Weg gibt
   */
  public static List<int[]> find(
      WorldView source,
      Area area,
      int startX,
      int startY,
      int startZ,
      int goalX,
      int goalY,
      int goalZ,
      int maxNodes) {
    if (!area.contains(startX, startZ) || !area.contains(goalX, goalZ)) {
      return null;
    }

    // Jede Zelle wird von mehreren Nachbarn aus abgefragt. Merken spart im Spiel viele Blockabfragen.
    WorldView world = new CachedWorld(source);

    Node start = new Node(startX, startY, startZ);
    start.estimate = heuristic(startX, startY, startZ, goalX, goalY, goalZ);

    HashMap<Long, Node> nodes = new HashMap<>();
    PriorityQueue<Node> open = new PriorityQueue<>();
    nodes.put(key(startX, startY, startZ), start);
    open.add(start);

    int expanded = 0;
    while (!open.isEmpty()) {
      Node current = open.poll();
      if (current.closed) {
        continue;
      }
      current.closed = true;

      if (current.x == goalX && current.y == goalY && current.z == goalZ) {
        return buildPath(current);
      }

      expanded++;
      if (expanded > maxNodes) {
        return null;
      }

      for (int direction = 0; direction < 4; direction++) {
        int nx = current.x + DX[direction];
        int nz = current.z + DZ[direction];
        if (!area.contains(nx, nz)) {
          continue;
        }

        int ny = Integer.MIN_VALUE;
        double stepCost = 0.0D;
        if (world.canStand(nx, current.y, nz)) {
          ny = current.y;
          stepCost = 1.0D;
        } else if (world.isFree(current.x, current.y + 2, current.z)
            && world.canStand(nx, current.y + 1, nz)) {
          // Einen Block hinauf: Der Spieler springt, dazu braucht er über dem Kopf Platz.
          ny = current.y + 1;
          stepCost = 2.2D;
        } else if (world.isFree(nx, current.y, nz) && world.isFree(nx, current.y + 1, nz)) {
          // Hinunter: Die Spalte muss bis zum Boden frei sein.
          for (int drop = 1; drop <= MAX_DROP; drop++) {
            if (world.canStand(nx, current.y - drop, nz)) {
              ny = current.y - drop;
              stepCost = 1.0D + 0.5D * drop;
              break;
            }
            if (!world.isFree(nx, current.y - drop, nz)) {
              break;
            }
          }
        }

        if (ny == Integer.MIN_VALUE) {
          continue;
        }

        double cost = current.cost + stepCost;
        long key = key(nx, ny, nz);
        Node next = nodes.get(key);
        if (next == null) {
          next = new Node(nx, ny, nz);
          nodes.put(key, next);
        } else if (next.closed || cost >= next.cost) {
          continue;
        }

        next.cost = cost;
        next.estimate = cost + heuristic(nx, ny, nz, goalX, goalY, goalZ);
        next.parent = current;
        open.add(next);
      }
    }

    return null;
  }

  /** Merkt sich die Antworten der Welt für die Dauer einer Suche. */
  private static final class CachedWorld implements WorldView {
    private final WorldView inner;
    private final HashMap<Long, Boolean> free = new HashMap<>();
    private final HashMap<Long, Boolean> stand = new HashMap<>();

    CachedWorld(WorldView inner) {
      this.inner = inner;
    }

    @Override
    public boolean isFree(int x, int y, int z) {
      long key = key(x, y, z);
      Boolean cached = this.free.get(key);
      if (cached == null) {
        cached = this.inner.isFree(x, y, z);
        this.free.put(key, cached);
      }
      return cached;
    }

    @Override
    public boolean canStand(int x, int y, int z) {
      long key = key(x, y, z);
      Boolean cached = this.stand.get(key);
      if (cached == null) {
        cached = this.inner.canStand(x, y, z);
        this.stand.put(key, cached);
      }
      return cached;
    }
  }

  private static double heuristic(int x, int y, int z, int goalX, int goalY, int goalZ) {
    return Math.abs(x - goalX) + Math.abs(z - goalZ) + Math.abs(y - goalY);
  }

  private static long key(int x, int y, int z) {
    return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) (y + 2048) & 0xFFFL);
  }

  private static List<int[]> buildPath(Node goal) {
    ArrayList<int[]> path = new ArrayList<>();
    for (Node node = goal; node != null; node = node.parent) {
      path.add(new int[] {node.x, node.y, node.z});
    }
    Collections.reverse(path);
    return path;
  }
}
