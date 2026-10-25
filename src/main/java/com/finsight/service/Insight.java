package com.finsight.service;

/**
 * A short observation about the user's money, such as "Food spending is up 18%".
 *
 * @param tone whether the observation is good news, bad news or neutral, so the UI can colour it
 */
public record Insight(Tone tone, String headline, String detail) {

    public enum Tone { POSITIVE, NEGATIVE, NEUTRAL }
}
