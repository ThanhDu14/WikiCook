import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  pointerWithin,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
  type Announcements,
  type CollisionDetection,
  type DragEndEvent,
  type DragStartEvent,
  type KeyboardCoordinateGetter,
} from '@dnd-kit/core';
import { useMemo, useRef, useState, type KeyboardEventHandler, type PointerEventHandler, type ReactNode } from 'react';
import type { SlotRef } from '../hooks/useWeekPlan';
import { dayOfMonth } from '../lib/week';
import { vi } from '../strings.vi';
import { DAYS, MAX_DISHES, MEALS, type Day, type Meal, type PlanItem, type WeekPlan } from '../types';
import { EmptySlot } from './EmptySlot';
import { Icon } from './Icon';
import { Overlay } from './Overlay';
import { SlotCard } from './SlotCard';

interface Props {
  week: WeekPlan;
  onAdd: (day: Day, meal: Meal) => void;
  onOpenItem: (day: Day, meal: Meal, item: PlanItem) => void;
  /** Turns on drag and drop (pointer and keyboard) of dishes between meals (T068). */
  enableDrag?: boolean;
  /** A dish dropped on another meal; it goes to the end of that meal. */
  onMove?: (itemId: number, to: SlotRef) => void;
  /** Shows "Xóa cả ngày" in the header of each day with dishes; asked for confirmation first (T069). */
  onClearDay?: (day: Day) => void;
}

interface DragData {
  day: Day;
  meal: Meal;
  itemId: number;
  name: string;
}

const slotId = (slot: SlotRef) => `${slot.day}/${slot.meal}`;
const slotName = (slot: SlotRef) => vi.slotOf(vi.dayLong[slot.day], vi.mealLong[slot.meal]);
const ARROWS: Record<string, [number, number]> = { ArrowRight: [1, 0], ArrowLeft: [-1, 0], ArrowDown: [0, 1], ArrowUp: [0, -1] };

/**
 * The week of W-MP01 (`wx-week`): days as columns (Monday first, today marked), meals as rows, each cell a
 * `wk-slot` holding the 0–4 dishes of that meal in order, with "Thêm món" until it is full (T046, FR-004). With
 * `enableDrag` a dish is dragged with the pointer or by its handle with the keyboard (Space, arrows, Space; Esc
 * cancels) to another meal; arrows step from meal to meal, so the keyboard never depends on the layout (T068).
 */
export function WeekGrid({ week, onAdd, onOpenItem, enableDrag = false, onMove, onClearDay }: Props) {
  const [confirmDay, setConfirmDay] = useState<Day | null>(null);
  /** Slot the keyboard is over while a dish is lifted with the keyboard; null for the pointer. */
  const keyboardOver = useRef<SlotRef | null>(null);

  const coordinateGetter = useMemo<KeyboardCoordinateGetter>(
    () => (event, { context, currentCoordinates }) => {
      const step = ARROWS[event.code] ?? ARROWS[event.key];
      const at = keyboardOver.current;
      if (!step || !at) return undefined;
      event.preventDefault();
      const day = DAYS.indexOf(at.day) + step[0];
      const meal = MEALS.indexOf(at.meal) + step[1];
      if (day < 0 || day >= DAYS.length || meal < 0 || meal >= MEALS.length) return undefined;
      const next = { day: DAYS[day], meal: MEALS[meal] };
      keyboardOver.current = next;
      // Go to the target cell, so the sensor scrolls it into view; without a layout (tests) nudge sideways so
      // the collisions are computed again.
      const rect = context.droppableRects.get(slotId(next));
      return rect && (rect.width || rect.height) ? { x: rect.left, y: rect.top } : { x: currentCoordinates.x + 1, y: currentCoordinates.y };
    },
    [],
  );
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor, { coordinateGetter }),
  );
  const collisionDetection = useMemo<CollisionDetection>(
    () => (args) => (keyboardOver.current ? [{ id: slotId(keyboardOver.current) }] : pointerWithin(args)),
    [],
  );
  const announcements = useMemo<Announcements>(() => {
    const name = (data: unknown) => (data as DragData | undefined)?.name ?? '';
    const target = (data: unknown) => slotName(data as SlotRef);
    return {
      onDragStart: ({ active }) => vi.dragPicked(name(active.data.current)),
      // the slot it was lifted from is not news
      onDragOver: ({ active, over }) =>
        over && over.id !== slotId(active.data.current as DragData) ? vi.dragOver(target(over.data.current)) : undefined,
      onDragEnd: ({ active, over }) =>
        over ? vi.dragDropped(name(active.data.current), target(over.data.current)) : vi.dragCancelled(name(active.data.current)),
      onDragCancel: ({ active }) => vi.dragCancelled(name(active.data.current)),
    };
  }, []);

  const start = (event: DragStartEvent) => {
    const from = event.active.data.current as DragData;
    keyboardOver.current = event.activatorEvent instanceof KeyboardEvent ? { day: from.day, meal: from.meal } : null;
  };
  const end = (event: DragEndEvent) => {
    keyboardOver.current = null;
    const from = event.active.data.current as DragData;
    const to = event.over?.data.current as SlotRef | undefined;
    if (to && slotId(to) !== slotId(from)) onMove?.(from.itemId, { day: to.day, meal: to.meal });
  };

  const grid = (
    <div className="overflow-x-auto pb-ds-2">
      <div role="table" aria-label={vi.pageTitle} className="wx-week" style={{ minWidth: 960, gridAutoRows: 'minmax(116px, auto)', gridTemplateRows: 'auto' }}>
        <div role="row" style={{ display: 'contents' }}>
          <span />
          {week.days.map((d) => {
            const isToday = d.date === week.today;
            const hasDishes = d.slots.some((s) => s.items.length > 0);
            return (
              <div
                key={d.day}
                role="columnheader"
                aria-current={isToday ? 'date' : undefined}
                className={`group ${isToday ? 'wx-week__day is-today' : 'wx-week__day'}`}
                style={{ position: 'relative' }}
              >
                <span className="wk-caption">{vi.dayShort[d.day]}</span>
                <strong>{dayOfMonth(d.date)}</strong>
                {onClearDay && hasDishes && (
                  <button
                    type="button"
                    className="wk-icon-btn wk-icon-btn--plain opacity-0 transition-opacity focus-visible:opacity-100 group-hover:opacity-100"
                    aria-label={vi.clearDayOf(vi.dayLong[d.day])}
                    onClick={() => setConfirmDay(d.day)}
                    style={{ position: 'absolute', top: 0, right: 0, width: 32, height: 32, minHeight: 32 }}
                  >
                    <Icon name="trash" size={16} />
                  </button>
                )}
              </div>
            );
          })}
        </div>
        {MEALS.map((meal, mealIndex) => (
          <div key={meal} role="row" style={{ display: 'contents' }}>
            <div role="rowheader" className="wx-week__meal">
              {vi.meal[meal]}
            </div>
            {week.days.map((d) => {
              const slot = { day: d.day, meal };
              const content = (
                <MealCell
                  slot={slot}
                  items={d.slots[mealIndex].items}
                  enableDrag={enableDrag}
                  onAdd={() => onAdd(d.day, meal)}
                  onOpenItem={(item) => onOpenItem(d.day, meal, item)}
                />
              );
              return enableDrag ? (
                <DroppableCell key={d.day} slot={slot}>
                  {content}
                </DroppableCell>
              ) : (
                <div key={d.day} role="cell" className="grid min-w-0">
                  {content}
                </div>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );

  return (
    <>
      {enableDrag ? (
        <DndContext
          sensors={sensors}
          collisionDetection={collisionDetection}
          accessibility={{ announcements, screenReaderInstructions: { draggable: vi.dragInstructions } }}
          onDragStart={start}
          onDragEnd={end}
          onDragCancel={() => (keyboardOver.current = null)}
        >
          {grid}
        </DndContext>
      ) : (
        grid
      )}
      {confirmDay && (
        <Overlay title={vi.clearDayConfirm(vi.dayLong[confirmDay])} variant="dialog" role="alertdialog" onClose={() => setConfirmDay(null)}>
          {(titleId) => (
            <>
              <h2 id={titleId} className="wk-h3">
                {vi.clearDayConfirm(vi.dayLong[confirmDay])}
              </h2>
              <p className="wk-muted">{vi.clearDayHint}</p>
              <div className="wk-dialog__actions">
                <button type="button" className="wk-btn wk-btn--secondary" data-autofocus onClick={() => setConfirmDay(null)}>
                  {vi.cancel}
                </button>
                <button
                  type="button"
                  className="wk-btn wk-btn--danger"
                  onClick={() => {
                    onClearDay?.(confirmDay);
                    setConfirmDay(null);
                  }}
                >
                  {vi.clearDay}
                </button>
              </div>
            </>
          )}
        </Overlay>
      )}
    </>
  );
}

function DroppableCell({ slot, children }: { slot: SlotRef; children: ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({ id: slotId(slot), data: slot });
  return (
    <div
      ref={setNodeRef}
      role="cell"
      className="grid min-w-0"
      style={isOver ? { outline: '2.5px solid var(--color-primary)', outlineOffset: 2, borderRadius: 14 } : undefined}
    >
      {children}
    </div>
  );
}

interface MealCellProps {
  slot: SlotRef;
  items: PlanItem[];
  enableDrag: boolean;
  onAdd: () => void;
  onOpenItem: (item: PlanItem) => void;
}

/** One meal of a day: its dishes in order and "Thêm món" until there are 4 (US1-11, FR-004). */
function MealCell({ slot, items, enableDrag, onAdd, onOpenItem }: MealCellProps) {
  if (items.length === 0) return <EmptySlot day={slot.day} meal={slot.meal} onAdd={onAdd} />;
  const dense = items.length > 1;
  return (
    <div className={items.some((i) => !i.available) ? 'wk-slot wk-slot--needs' : 'wk-slot'} style={{ gap: 0, padding: 8 }}>
      {items.map((item, index) => (
        <div key={item.id} style={index > 0 ? { borderTop: '1px dashed var(--color-border)', marginTop: 10, paddingTop: 10 } : undefined}>
          {enableDrag ? (
            <DraggableCard slot={slot} item={item} dense={dense} onOpen={() => onOpenItem(item)} />
          ) : (
            <SlotCard day={slot.day} meal={slot.meal} item={item} dense={dense} onOpen={() => onOpenItem(item)} />
          )}
        </div>
      ))}
      {items.length < MAX_DISHES && (
        <button
          type="button"
          className="sx-row wk-caption wk-muted"
          aria-label={`${vi.addDish}: ${vi.dayLong[slot.day]}, ${vi.mealLong[slot.meal]}`}
          onClick={onAdd}
          style={{ gap: 4, marginTop: 6, padding: '4px 0', border: 0, background: 'transparent', cursor: 'pointer' }}
        >
          <Icon name="plus" size={16} />
          {vi.addDish}
        </button>
      )}
    </div>
  );
}

function DraggableCard({ slot, item, dense, onOpen }: { slot: SlotRef; item: PlanItem; dense: boolean; onOpen: () => void }) {
  const data: DragData = { ...slot, itemId: item.id, name: item.recipeName };
  const { setActivatorNodeRef, setNodeRef, listeners, attributes, isDragging } = useDraggable({ id: `item-${item.id}`, data });
  return (
    <div ref={setNodeRef} style={isDragging ? { opacity: 0.5 } : undefined}>
      <SlotCard
        day={slot.day}
        meal={slot.meal}
        item={item}
        dense={dense}
        onOpen={onOpen}
        // the pointer drags the whole dish; the keyboard lifts it with the handle
        dragProps={{ onPointerDown: listeners?.onPointerDown as PointerEventHandler<HTMLDivElement> | undefined }}
        dragHandle={
          <button
            ref={setActivatorNodeRef}
            type="button"
            className="wk-icon-btn wk-icon-btn--plain"
            {...attributes}
            onKeyDown={listeners?.onKeyDown as KeyboardEventHandler<HTMLButtonElement> | undefined}
            aria-label={vi.dragHandle(item.recipeName)}
            style={{ width: 32, height: 32, minHeight: 32, background: 'var(--color-surface)', cursor: 'grab' }}
          >
            <Icon name="move" size={16} />
          </button>
        }
      />
    </div>
  );
}
