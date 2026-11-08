import { http, HttpResponse } from 'msw';
import type { SearchOptions } from '@/features/search/types';

export const mockSearchOptions: SearchOptions = {
  cuisines: [
    { slug: 'viet-nam', name: 'Việt Nam' },
    { slug: 'nhat-ban', name: 'Nhật Bản' },
  ],
  categories: [
    { slug: 'mon-chinh', name: 'Món chính' },
    { slug: 'canh-sup', name: 'Canh / Súp' },
  ],
  methods: [
    { slug: 'chien', name: 'Chiên' },
    { slug: 'luoc', name: 'Luộc' },
    { slug: 'nuong', name: 'Nướng' },
  ],
  timeRanges: ['LE15', 'LE30', 'LE60', 'GT60'],
  difficulties: ['EASY', 'MEDIUM', 'HARD'],
  diets: ['VEGETARIAN', 'VEGAN', 'PESCATARIAN'],
  sorts: ['RELEVANCE', 'NEWEST', 'TOP_RATED', 'QUICKEST'],
};

export const mockFeaturedRecipes = {
  items: [
    {
      id: 101,
      title: 'Gà kho gừng',
      coverImageUrl: 'https://cdn.example.com/recipes/101.jpg',
      totalTimeMinutes: 25,
      difficulty: 'EASY',
      ratingAvg: 4.7,
      reviewCount: 88,
      cuisine: 'Việt Nam',
      conflicts: [],
    },
    {
      id: 102,
      title: 'Cháo gà hành phi',
      coverImageUrl: null,
      totalTimeMinutes: 30,
      difficulty: 'EASY',
      ratingAvg: null,
      reviewCount: 0,
      cuisine: 'Việt Nam',
      conflicts: [],
    },
  ],
};

export const searchHandlers = [
  http.get('/api/v1/recipes/search/options', () => {
    return HttpResponse.json(mockSearchOptions);
  }),

  http.get('/api/v1/recipes/featured', ({ request }) => {
    const url = new URL(request.url);
    const limit = Number(url.searchParams.get('limit')) || 10;
    return HttpResponse.json({
      items: mockFeaturedRecipes.items.slice(0, limit),
    });
  }),

  http.get('/api/v1/recipes/search', ({ request }) => {
    const url = new URL(request.url);
    const q = url.searchParams.get('q');
    const showHidden = url.searchParams.get('showHidden') === 'true';
    const page = Math.max(1, Number(url.searchParams.get('page')) || 1);
    const sortParam = url.searchParams.get('sort');
    const effectiveSort = sortParam || (q ? 'RELEVANCE' : 'NEWEST');

    if (q === 'loi') {
      return HttpResponse.json(
        {
          type: 'about:blank',
          title: 'Internal Server Error',
          status: 500,
          detail: 'Đã có lỗi xảy ra',
          code: 'SERVER_ERROR',
        },
        { status: 500 },
      );
    }

    if (q === 'xyzabc') {
      return HttpResponse.json({
        items: [],
        page: 1,
        pageSize: 20,
        totalItems: 0,
        totalPages: 0,
        sort: effectiveSort,
        keyword: 'xyzabc',
        keywordTruncated: false,
        appliedFilters: {
          time: null,
          cuisines: [],
          categories: [],
          difficulties: [],
          methods: [],
          diet: null,
        },
        dietary: {
          profileAvailable: false,
          applied: false,
          hiddenCount: 0,
          showingHidden: false,
        },
      });
    }

    if (q === 'goi ga' && !showHidden) {
      return HttpResponse.json({
        items: [],
        page: 1,
        pageSize: 20,
        totalItems: 0,
        totalPages: 0,
        sort: effectiveSort,
        keyword: 'goi ga',
        keywordTruncated: false,
        appliedFilters: {
          time: null,
          cuisines: [],
          categories: [],
          difficulties: [],
          methods: [],
          diet: null,
        },
        dietary: {
          profileAvailable: true,
          applied: true,
          hiddenCount: 4,
          showingHidden: false,
        },
      });
    }

    if (showHidden) {
      return HttpResponse.json({
        items: [
          {
            id: 230,
            title: 'Gỏi gà bắp cải',
            coverImageUrl: 'https://cdn.example.com/recipes/230.jpg',
            totalTimeMinutes: 25,
            difficulty: 'EASY',
            ratingAvg: 4.6,
            reviewCount: 54,
            cuisine: 'Việt Nam',
            conflicts: [
              { type: 'ALLERGEN', name: 'Đậu phộng' },
              { type: 'EXCLUDED_INGREDIENT', name: 'Ngò rí' },
            ],
          },
        ],
        page,
        pageSize: 20,
        totalItems: 48,
        totalPages: 3,
        sort: effectiveSort,
        keyword: q ?? 'ga',
        keywordTruncated: false,
        appliedFilters: {
          time: null,
          cuisines: [],
          categories: [],
          difficulties: [],
          methods: [],
          diet: null,
        },
        dietary: {
          profileAvailable: true,
          applied: false,
          hiddenCount: 0,
          showingHidden: true,
        },
      });
    }

    // Default withResults (SR-02)
    const cuisines = url.searchParams.getAll('cuisine').map((slug) => ({
      slug,
      name: slug === 'viet-nam' ? 'Việt Nam' : slug === 'nhat-ban' ? 'Nhật Bản' : slug,
    }));
    const categories = url.searchParams.getAll('category').map((slug) => ({
      slug,
      name: slug === 'mon-chinh' ? 'Món chính' : slug === 'canh-sup' ? 'Canh / Súp' : slug,
    }));
    const methods = url.searchParams.getAll('method').map((slug) => ({
      slug,
      name: slug === 'chien' ? 'Chiên' : slug === 'luoc' ? 'Luộc' : slug === 'nuong' ? 'Nướng' : slug,
    }));
    const difficulties = url.searchParams.getAll('difficulty');
    const time = url.searchParams.get('time') || null;
    const diet = url.searchParams.get('diet') || null;

    const defaultItems = [
      {
        id: 101,
        title: 'Gà kho gừng',
        coverImageUrl: 'https://cdn.example.com/recipes/101.jpg',
        totalTimeMinutes: 25,
        difficulty: 'EASY',
        ratingAvg: 4.7,
        reviewCount: 88,
        cuisine: 'Việt Nam',
        conflicts: [],
      },
      {
        id: 102,
        title: 'Cháo gà hành phi',
        coverImageUrl: null,
        totalTimeMinutes: 30,
        difficulty: 'EASY',
        ratingAvg: null,
        reviewCount: 0,
        cuisine: 'Việt Nam',
        conflicts: [],
      },
    ];

    const page2Items = [
      {
        id: 103,
        title: 'Gà hấp lá chanh',
        coverImageUrl: null,
        totalTimeMinutes: 35,
        difficulty: 'MEDIUM' as const,
        ratingAvg: 4.8,
        reviewCount: 15,
        cuisine: 'Việt Nam',
        conflicts: [],
      },
    ];

    return HttpResponse.json({
      items: page === 1 ? defaultItems : page2Items,
      page,
      pageSize: 20,
      totalItems: 42,
      totalPages: 3,
      sort: effectiveSort,
      keyword: q ?? '',
      keywordTruncated: false,
      appliedFilters: {
        time,
        cuisines,
        categories,
        difficulties,
        methods,
        diet,
      },
      dietary: {
        profileAvailable: true,
        applied: true,
        hiddenCount: 6,
        showingHidden: false,
      },
    });
  }),
];
