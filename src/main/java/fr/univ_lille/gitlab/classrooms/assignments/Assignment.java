package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.adapters.jpa.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Assignment {

    @Id
    private UUID id = UUID.randomUUID();

    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<ClassroomUser> students = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private AssignmentType type;

    @ManyToOne
    private ClassroomEntity classroom;

    private ZonedDateTime dueDate;

    private boolean autoArchive = false;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status = AssignmentStatus.OPENED;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<ClassroomUser> getStudents() {
        return students;
    }

    public void setStudents(Set<ClassroomUser> students) {
        this.students = students;
    }

    public void accept(ClassroomUser student){
        this.students.add(student);
    }

    public AssignmentType getType() {
        return type;
    }

    public void setType(AssignmentType type) {
        this.type = type;
    }

    public ClassroomEntity getClassroom() {
        return classroom;
    }

    public void setClassroom(ClassroomEntity classroom) {
        this.classroom = classroom;
    }

    public ZonedDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(ZonedDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isAutoArchive() {
        return autoArchive;
    }

    public void setAutoArchive(boolean autoArchive) {
        this.autoArchive = autoArchive;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }
}
