package fr.lesprojetscagnottes.core.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@MappedSuperclass
@SuperBuilder
public class GenericModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Min(0)
    @NotNull
    @NotEmpty
    protected Long id = 0L;

    public GenericModel() {}

    public GenericModel(GenericModel model) {
        if(model != null) {
            this.id = model.getId();
        } else {
            this.id = 0L;
        }
    }

    @Override
    public String toString() {
        return "GenericModel{" +
                "id=" + id +
                '}';
    }
}
