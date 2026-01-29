package com.nucleareconomy;

import java.util.UUID;

public record ChequeRecord(UUID id, EconomyType economy, double value, String creator, boolean redeemed) {
}
