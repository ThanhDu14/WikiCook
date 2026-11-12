import type { RecipeSnapshot } from '../../domain/sessionModel';

export const mockPhoBoSnapshot: RecipeSnapshot = {
  recipeId: 'recipe-pho-bo-101',
  statusAtLoad: 'PUBLISHED',
  title: 'Phở bò truyền thống Hà Nội',
  baseServings: 4,
  loadedAt: 1731300000000,
  ingredients: [
    {
      rowId: 'ing-1',
      name: 'Xương ống bò',
      quantity: 1.5,
      amountText: '1.5 kg',
      unitName: 'kg',
      note: 'Rửa sạch, chặt khúc',
      groupName: 'Nước dùng',
      isOptional: false,
      position: 1,
    },
    {
      rowId: 'ing-2',
      name: 'Thịt bò phi lê',
      quantity: 400,
      amountText: '400 g',
      unitName: 'g',
      note: 'Thái lát mỏng',
      groupName: 'Thịt & Bánh phở',
      isOptional: false,
      position: 2,
    },
    {
      rowId: 'ing-3',
      name: 'Bánh phở tươi',
      quantity: 500,
      amountText: '500 g',
      unitName: 'g',
      note: null,
      groupName: 'Thịt & Bánh phở',
      isOptional: false,
      position: 3,
    },
    {
      rowId: 'ing-4',
      name: 'Hành tây, hoa hồi, thảo quả',
      quantity: null,
      amountText: 'vừa đủ',
      unitName: 'to_taste',
      note: 'Nướng thơm',
      groupName: 'Gia vị hương liệu',
      isOptional: false,
      position: 4,
    },
  ],
  steps: [
    {
      stepId: 'step-1',
      stepNumber: 1,
      instructionText:
        'Trần sơ xương bò với nước sôi có gừng đập dập trong 5 phút để khử mùi hôi, sau đó vớt ra rửa sạch lại với nước lạnh.',
      images: [
        {
          url: 'https://images.wikicook.local/pho-bo-step-1.jpg',
          alt: 'Ảnh minh họa bước 1',
          position: 1,
        },
      ],
      timerSeconds: 300, // 5 minutes author timer
      ingredientRowIds: ['ing-1'],
    },
    {
      stepId: 'step-2',
      stepNumber: 2,
      instructionText:
        'Nướng hành tây, gừng, hoa hồi và thảo quả trên lửa vừa trong 3-5 phút cho dậy mùi thơm nức.',
      images: [],
      timerSeconds: null, // range in text: 3-5 phút
      ingredientRowIds: ['ing-4'],
    },
    {
      stepId: 'step-3',
      stepNumber: 3,
      instructionText:
        'Cho xương bò và túi gia vị hương liệu vào nồi to, thêm 3 lít nước và ninh nhỏ lửa trong 1 giờ 30 phút.',
      images: [
        {
          url: 'https://images.wikicook.local/pho-bo-step-3.jpg',
          alt: 'Ảnh minh họa bước 3',
          position: 1,
        },
      ],
      timerSeconds: null, // compound in text: 1 giờ 30 phút (5400s)
      ingredientRowIds: ['ing-1', 'ing-4'],
    },
    {
      stepId: 'step-4',
      stepNumber: 4,
      instructionText:
        'Chần bánh phở tươi qua nước sôi rồi chia đều ra các tô. Xếp thịt bò phi lê tái lên trên.',
      images: [],
      timerSeconds: null,
      // No ingredientRowIds here to test US2-AC6 fallback to complete ingredient drawer
    },
    {
      stepId: 'step-5',
      stepNumber: 5,
      instructionText:
        'Chan nước dùng thật sôi ngập bánh phở và thịt bò. Rắc hành hoa, ngò gai thái nhỏ lên trên và thưởng thức nóng.',
      images: [
        {
          url: 'https://images.wikicook.local/pho-bo-step-5.jpg',
          alt: 'Ảnh minh họa bước 5',
          position: 1,
        },
      ],
      timerSeconds: null,
    },
  ],
};
