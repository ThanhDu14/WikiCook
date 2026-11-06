import { dishName } from '../lib/dishName';

describe('dishName', () => {
  it('keeps only the dish before the "how to" part of a title', () => {
    expect(dishName('Bánh cống Sóc Trăng: cách làm giòn rụm tại nhà')).toBe('Bánh cống Sóc Trăng');
    expect(dishName('Bánh ít tôm thịt : cách làm bánh ít trần dẻo mềm')).toBe('Bánh ít tôm thịt');
  });

  it('leaves titles without such a part as they are', () => {
    expect(dishName('Canh chua cá lóc')).toBe('Canh chua cá lóc');
    expect(dishName('  Phở bò  ')).toBe('Phở bò');
  });

  it('never returns an empty name', () => {
    expect(dishName(': chỉ có mô tả')).toBe(': chỉ có mô tả');
  });
});
