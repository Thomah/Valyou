package fr.lesprojetscagnottes.core.queue;

import fr.lesprojetscagnottes.core.entity.Donation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
public class DonationOperation {

    private DonationOperationType type;
    private Donation donation;

    public DonationOperation(Donation donation, DonationOperationType type) {
        this.donation = donation;
        this.type = type;
    }

}
