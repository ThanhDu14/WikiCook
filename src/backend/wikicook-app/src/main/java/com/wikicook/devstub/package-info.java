/**
 * Development stand-ins for beans other specs will provide: the signed-in user (010) and the dietary profile (011).
 * Off unless {@code wikicook.dev-stubs.enabled=true}; every bean steps aside when the real one exists. Not
 * production code: delete the package once 010 and 011 land. (The meal plan write facade stub left when 030
 * T100 shipped {@code MealPlanWriteService}.)
 */
package com.wikicook.devstub;
