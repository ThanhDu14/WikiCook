import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { DayNutritionBadge } from '../components/DayNutritionBadge';
import { DayView } from '../components/DayView';
import { WeekGrid } from '../components/WeekGrid';
import type { WeekPlan } from '../types';
import { item, week } from './fixtures';

const DISCLAIMER = 'Số liệu dinh dưỡng do AI ước tính, chỉ mang tính tham khảo';

/** The fixture week with nutrition on Wednesday. */
function withNutrition(plan: WeekPlan, missingDishes = 1): WeekPlan {
  return {
    ...plan,
    days: plan.days.map((d) => (d.day === 'WEDNESDAY' ? { ...d, nutrition: { kcalPerPerson: 1240.4, includedDishes: 2, missingDishes } } : d)),
  };
}

describe('DayNutritionBadge (T096)', () => {
  it('shows the calories per person, the dishes without data and the AI disclaimer (FR-032)', () => {
    render(<DayNutritionBadge hasDishes nutrition={{ kcalPerPerson: 1240.4, includedDishes: 2, missingDishes: 1 }} />);

    expect(screen.getByText(/~1\.240 kcal\/người/)).toBeInTheDocument();
    expect(screen.getByText(/1 món chưa có số liệu/)).toBeInTheDocument();
    expect(screen.getByText(DISCLAIMER)).toBeInTheDocument();
  });

  it('leaves out the missing count when every dish has data', () => {
    render(<DayNutritionBadge hasDishes nutrition={{ kcalPerPerson: 800, includedDishes: 3, missingDishes: 0 }} />);

    expect(screen.getByText(/~800 kcal\/người/)).toBeInTheDocument();
    expect(screen.queryByText(/chưa có số liệu/)).not.toBeInTheDocument();
  });

  it('says there is no nutrition data yet for a day with dishes but no estimate', () => {
    render(<DayNutritionBadge hasDishes nutrition={null} />);

    expect(screen.getByText('Chưa có số liệu dinh dưỡng')).toBeInTheDocument();
  });

  it('shows nothing for a day without dishes', () => {
    const { container } = render(<DayNutritionBadge hasDishes={false} nutrition={null} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('sits in the header of the day on the day view (FR-032)', () => {
    render(
      <MemoryRouter>
        <DayView week={withNutrition(week({ 'WEDNESDAY/LUNCH': item() }))} onAdd={vi.fn()} onOpenItem={vi.fn()} />
      </MemoryRouter>,
    );

    const header = screen.getByRole('heading', { level: 2, name: 'Thứ Tư, 21/10' }).closest('header');
    expect(header).not.toBeNull();
    expect(within(header as HTMLElement).getByText(/~1\.240 kcal\/người/)).toBeInTheDocument();
    expect(within(header as HTMLElement).getByText(DISCLAIMER)).toBeInTheDocument();
  });

  it('sits in the column header of its day on the week grid, with the disclaimer once', () => {
    render(
      <MemoryRouter>
        <WeekGrid week={withNutrition(week({ 'WEDNESDAY/LUNCH': item(), 'MONDAY/LUNCH': item({ id: 2 }) }))} onAdd={vi.fn()} onOpenItem={vi.fn()} />
      </MemoryRouter>,
    );

    const [monday, , wednesday] = screen.getAllByRole('columnheader');
    expect(within(wednesday).getByText(/~1\.240 kcal/)).toBeInTheDocument();
    expect(within(monday).getByText('Chưa có số liệu dinh dưỡng')).toBeInTheDocument();
    expect(screen.getAllByText(DISCLAIMER)).toHaveLength(1);
  });
});
