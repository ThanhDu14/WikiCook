package com.wikicook.mealplan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Objects;

/**
 * {@code meal_plan_items}: the dish in one slot (data-model 1). {@code recipeId} becomes {@code null} when the
 * recipe is deleted; the name snapshot keeps what the user saw (research R8). {@code version} detects edits made
 * elsewhere (research R4).
 */
@Entity
@Table(name = "meal_plan_items")
public class MealPlanItem {

    /** Column size of {@code recipe_name_snapshot}. */
    public static final int NAME_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false, updatable = false)
    private MealPlan mealPlan;

    @Convert(converter = DayOfWeekConverter.class)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek day;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal", nullable = false, length = 10)
    private Meal meal;

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "recipe_name_snapshot", nullable = false, length = NAME_MAX)
    private String recipeNameSnapshot;

    @Column(name = "servings", nullable = false)
    private short servings;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private ItemSource source;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    protected MealPlanItem() {
    }

    public MealPlanItem(MealPlan mealPlan, DayOfWeek day, Meal meal, long recipeId, String recipeName, int servings,
                        ItemSource source) {
        this.mealPlan = Objects.requireNonNull(mealPlan, "mealPlan");
        this.day = Objects.requireNonNull(day, "day");
        this.meal = Objects.requireNonNull(meal, "meal");
        setDish(recipeId, recipeName, servings, source);
    }

    /** Puts another dish in this slot (replace, swap). */
    public void setDish(Long recipeId, String recipeName, int servings, ItemSource source) {
        this.recipeId = recipeId;
        this.recipeNameSnapshot = snapshot(recipeName);
        this.servings = (short) servings;
        this.source = Objects.requireNonNull(source, "source");
    }

    /** Moves this item to another slot of the same week. */
    public void moveTo(DayOfWeek newDay, Meal newMeal) {
        this.day = Objects.requireNonNull(newDay, "day");
        this.meal = Objects.requireNonNull(newMeal, "meal");
    }

    public void setServings(int servings) {
        this.servings = (short) servings;
    }

    public void touch(Instant now) {
        updatedAt = now;
    }

    static String snapshot(String name) {
        String value = name == null || name.isBlank() ? "?" : name.strip();
        return value.length() <= NAME_MAX ? value : value.substring(0, NAME_MAX);
    }

    public Long getId() {
        return id;
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public Meal getMeal() {
        return meal;
    }

    public SlotKey slot() {
        return new SlotKey(day, meal);
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public String getRecipeNameSnapshot() {
        return recipeNameSnapshot;
    }

    public short getServings() {
        return servings;
    }

    public ItemSource getSource() {
        return source;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
