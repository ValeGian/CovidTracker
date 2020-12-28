package it.unipi.dii.inginf.dsmt.covidtracker.enums;

public enum MessageType {
    NO_ACTION_REQUEST,
    PING,
    PONG,
    REGISTRY_CLOSURE_REQUEST,
    AGGREGATION_REQUEST,
    AGGREGATION_RESPONSE,
    DAILY_REPORT,
    NEW_DATA,
    CONNECTION_REQUEST,
    CONNECTION_REFUSED,
    CONNECTION_ACCEPTED;
}
