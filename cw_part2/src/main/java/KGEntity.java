import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class KGEntity implements Comparable<KGEntity> {
    private String id;
    private String name;
    private String description;
    private Set<String> types = new HashSet<>();
    private double score;

    public KGEntity() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void addType(String type) {
        types.add(type);
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public int compareTo(KGEntity other) {
        return Double.compare(other.getScore(), this.getScore()); // higher score = higher rank
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KGEntity)) return false;
        KGEntity entity = (KGEntity) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "[KGEntity] id: " + id + ", name: " + name + ", score: " + score + ", desc: " + description;
    }
}
