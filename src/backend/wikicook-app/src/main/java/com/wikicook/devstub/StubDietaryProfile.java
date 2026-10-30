package com.wikicook.devstub;

import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;

/** The same configured profile ({@code wikicook.dev-stubs.profile.*}) for every user. */
public class StubDietaryProfile implements DietaryProfilePort {

    private final PlannerProfile profile;

    public StubDietaryProfile(PlannerProfile profile) {
        this.profile = profile;
    }

    @Override
    public PlannerProfile findByUserId(long userId) {
        return profile;
    }
}
