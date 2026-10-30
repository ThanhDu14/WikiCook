package com.wikicook.mealplan.port;

/**
 * The dietary profile the planners need (030 contracts/integration.md 1); implemented by the profile module
 * (spec 011). Until then the dev stubs ({@code com.wikicook.devstub}) provide one.
 */
public interface DietaryProfilePort {

    PlannerProfile findByUserId(long userId);
}
