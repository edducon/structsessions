package com.infosecconference.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "jury_id", "participant_id"}))
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jury_id", nullable = false)
    private ConferenceUser juryMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ConferenceUser participant;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal value;

    @Column(length = 2048)
    private String comment;

    public Score() {
    }

    public Score(Activity activity, ConferenceUser juryMember, ConferenceUser participant, BigDecimal value) {
        this.activity = activity;
        this.juryMember = juryMember;
        this.participant = participant;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public ConferenceUser getJuryMember() {
        return juryMember;
    }

    public void setJuryMember(ConferenceUser juryMember) {
        this.juryMember = juryMember;
    }

    public ConferenceUser getParticipant() {
        return participant;
    }

    public void setParticipant(ConferenceUser participant) {
        this.participant = participant;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Score score)) return false;
        return Objects.equals(activity, score.activity) &&
                Objects.equals(juryMember, score.juryMember) &&
                Objects.equals(participant, score.participant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activity, juryMember, participant);
    }
}
