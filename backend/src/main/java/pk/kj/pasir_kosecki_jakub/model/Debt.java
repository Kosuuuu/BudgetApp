package pk.kj.pasir_kosecki_jakub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "debts")
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "debtor_id", nullable = false)
    private User debtor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creditor_id", nullable = false)
    private User creditor;

    @Column(nullable = false)
    private Double amount;

    private String title;

    @Column(nullable = false)
    private boolean paidByDebtor = false;

    @Column(nullable = false)
    private boolean confirmedByCreditor = false;

    @Transient
    public Long getGroupId() {
        return group != null ? group.getId() : null;
    }

    @Transient
    public Long getDebtorId() {
        return debtor != null ? debtor.getId() : null;
    }

    @Transient
    public Long getCreditorId() {
        return creditor != null ? creditor.getId() : null;
    }
}