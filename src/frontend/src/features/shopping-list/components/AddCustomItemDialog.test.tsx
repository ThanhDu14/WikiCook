import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AddCustomItemDialog } from './AddCustomItemDialog';

function setup(onSubmit = vi.fn().mockResolvedValue(undefined)) {
  const onClose = vi.fn();
  render(<AddCustomItemDialog onSubmit={onSubmit} onClose={onClose} />);
  return { onSubmit, onClose, user: userEvent.setup() };
}

describe('AddCustomItemDialog (SL-04)', () => {
  it('renders the SL-04 form with the General aisle preselected', () => {
    setup();

    expect(screen.getByRole('dialog', { name: 'Thêm món' })).toBeInTheDocument();
    expect(screen.getByLabelText('Tên món')).toHaveFocus();
    expect(screen.getByLabelText('Quầy')).toHaveDisplayValue('Đồ dùng khác');
    expect(screen.getByText('Món không có trong danh mục sẽ vào quầy “Đồ dùng khác”.')).toBeInTheDocument();
  });

  it('submits name, quantity, unit and the chosen aisle', async () => {
    const { onSubmit, onClose, user } = setup();

    await user.type(screen.getByLabelText('Tên món'), 'Nước mắm');
    await user.type(screen.getByLabelText('Số lượng'), '1,5');
    await user.selectOptions(screen.getByLabelText('Đơn vị'), 'chai');
    // user-event matches option innerHTML, where "&" is "&amp;", so pick the option element itself
    await user.selectOptions(screen.getByLabelText('Quầy'), screen.getByRole('option', { name: 'Gia vị & Đồ khô' }));
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách' }));

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Nước mắm', quantity: 1.5, unit: 'chai', aisleId: 4 });
    expect(onClose).toHaveBeenCalled();
  });

  it('lets quantity and unit stay empty', async () => {
    const { onSubmit, user } = setup();

    await user.type(screen.getByLabelText('Tên món'), 'Nước rửa chén');
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách' }));

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Nước rửa chén', quantity: undefined, unit: undefined, aisleId: 6 });
  });

  it('requires a name', async () => {
    const { onSubmit, user } = setup();

    await user.type(screen.getByLabelText('Tên món'), '   ');
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách' }));

    expect(screen.getByText('Nhập tên món cần mua.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('rejects a zero or non-numeric quantity', async () => {
    const { onSubmit, user } = setup();

    await user.type(screen.getByLabelText('Tên món'), 'Trứng');
    await user.type(screen.getByLabelText('Số lượng'), '0');
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách' }));

    expect(screen.getByText('Số lượng phải là số lớn hơn 0.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('caps the name at 100 characters', () => {
    setup();

    expect(screen.getByLabelText('Tên món')).toHaveAttribute('maxLength', '100');
  });

  it('sends one item when Enter is pressed twice while saving', async () => {
    let finish: () => void = () => {};
    const onSubmit = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    const { user } = setup(onSubmit);

    await user.type(screen.getByLabelText('Tên món'), 'Nước mắm');
    await user.keyboard('{Enter}{Enter}');
    await act(async () => finish());

    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  it('stays open when saving fails', async () => {
    const { onClose, user } = setup(vi.fn().mockRejectedValue(new Error('422')));

    await user.type(screen.getByLabelText('Tên món'), 'Giấy bạc');
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách' }));

    expect(onClose).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog', { name: 'Thêm món' })).toBeInTheDocument();
  });

  it('closes on Escape', async () => {
    const { onClose, user } = setup();

    await user.keyboard('{Escape}');

    expect(onClose).toHaveBeenCalled();
  });
});
