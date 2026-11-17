/**
 * Development stand-ins for beans other specs will provide: the signed-in user (010; the real one is {@code SecurityContextCurrentAccount} now).
 * Off unless {@code wikicook.dev-stubs.enabled=true}; every bean steps aside when the real one exists. Not
 * production code: the dietary profile stub went away with spec 011 ({@code MealPlanDietaryAdapter} is the real port). (The meal plan write facade stub left when 030
 * T100 shipped {@code MealPlanWriteService}.)
 */
package com.wikicook.devstub;
