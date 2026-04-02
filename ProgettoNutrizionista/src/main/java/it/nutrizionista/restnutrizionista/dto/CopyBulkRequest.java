package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class CopyBulkRequest {

    @NotEmpty(message = "Specificare almeno un cliente di destinazione")
    @Size(max = 50, message = "Massimo 50 clienti per operazione")
    private List<Long> targetClienteIds;

    private boolean force = false;

    public List<Long> getTargetClienteIds() {
        return targetClienteIds;
    }

    public void setTargetClienteIds(List<Long> targetClienteIds) {
        this.targetClienteIds = targetClienteIds;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
