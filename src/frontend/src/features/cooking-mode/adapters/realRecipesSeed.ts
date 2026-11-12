import type { RecipeSnapshot } from "../domain/sessionModel";

/**
 * Real recipes extracted directly from Docker PostgreSQL (recipes table ID 6, 7, 9)
 * for authentic end-to-end testing of Feature 050 Cooking Mode.
 */
export const realPostgresRecipes: RecipeSnapshot[] = [
  {
    "recipeId": "6",
    "statusAtLoad": "PUBLISHED",
    "title": "Cách nấu cháo thịt bò hạt sen đơn giản, thơm ngon và giúp giải nhiệt",
    "baseServings": 2,
    "loadedAt": 1731300000000,
    "ingredients": [
      {
        "rowId": "ing-10976",
        "ingredientId": "871",
        "name": "Gạo nếp",
        "quantity": 100,
        "amountText": "100 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 1
      },
      {
        "rowId": "ing-10977",
        "ingredientId": "875",
        "name": "Gạo tẻ",
        "quantity": 100,
        "amountText": "100 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 2
      },
      {
        "rowId": "ing-10978",
        "ingredientId": "1135",
        "name": "Hạt sen tươi",
        "quantity": 150,
        "amountText": "150 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 3
      },
      {
        "rowId": "ing-10979",
        "ingredientId": "1",
        "name": "Thịt bò",
        "quantity": 300,
        "amountText": "300 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 4
      },
      {
        "rowId": "ing-10980",
        "ingredientId": "8",
        "name": "Trứng gà",
        "quantity": 1,
        "amountText": "1 quả",
        "unitName": "quả",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 5
      },
      {
        "rowId": "ing-10981",
        "ingredientId": "1057",
        "name": "Hành tím",
        "quantity": 2,
        "amountText": "2 củ",
        "unitName": "củ",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 6
      },
      {
        "rowId": "ing-10982",
        "ingredientId": "5",
        "name": "Tỏi",
        "quantity": 4,
        "amountText": "4 tép",
        "unitName": "tép",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 7
      },
      {
        "rowId": "ing-10983",
        "ingredientId": "1006",
        "name": "Hành lá",
        "quantity": 3,
        "amountText": "3 nhánh",
        "unitName": "nhánh",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 8
      },
      {
        "rowId": "ing-10984",
        "ingredientId": "622",
        "name": "Dầu ăn",
        "quantity": 3,
        "amountText": "3 muỗng canh",
        "unitName": "muỗng canh",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 9
      },
      {
        "rowId": "ing-10985",
        "ingredientId": "833",
        "name": "Đường phèn",
        "quantity": 1,
        "amountText": "1 muỗng canh",
        "unitName": "muỗng canh",
        "note": null,
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 10
      },
      {
        "rowId": "ing-10986",
        "ingredientId": "938",
        "name": "Gia vị thông dụng",
        "quantity": null,
        "amountText": "1 ít",
        "unitName": "vừa đủ",
        "note": "hạt nêm/ đường cát/ bột ngọt/ tiêu xay",
        "groupName": "Nguyên liệu (Cho 2 - 3 người)",
        "isOptional": false,
        "position": 11
      }
    ],
    "steps": [
      {
        "stepId": "step-6731",
        "stepNumber": 1,
        "instructionText": "Bạn vo sạch hỗn hợp gạo nếp và gạo tẻ với nước khoảng 2 - 3 lần cho sạch bẩn.\nBắc nồi nước lên bếp, khi nước sôi thì cho phần gạo vừa vo và 1 muỗng canh dầu ăn vào đun với lửa nhỏ.\nĐể cháo không bị dính ở đáy nồi, bạn lưu ý cần đảo cháo thật đều tay.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-1-nau-chao-trang-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-002.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-003.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-004.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6732",
        "stepNumber": 2,
        "instructionText": "Thịt bò mua về rửa sạch với nước nhiều lần, để ráo rồi cắt lát mỏng sau đó băm nhuyễn và cho vào chén.\nMẹo: Cách sơ chế thịt bò sạch và không hôi:Cách 1:Ngâm ngập miếng thịt bò trong rượu trắng khoảng 15 phút rồi rửa sạch lại với nước.Cách 2:Để thịt bò không bị mất chất, bạn có thể dùng khăn sạch ẩm lau chùi sơ qua miếng thịt bò một lần rồi đem đi chế biến.Cách 3:Ngoài ra bạn có thể nướng chín 1 củ gừng rồi giã nhuyễn và chà xát lên miếng thịt bò sau đó rửa sạch lại với nước.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-2-so-che-thit-bo-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-006.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          }
        ]
      },
      {
        "stepId": "step-6733",
        "stepNumber": 3,
        "instructionText": "Hạt sen tách bỏ lớp vỏ xanh bên ngoài và phần tim sen bên trong rồi rửa sạch với nước.\nTỏi và hành tím lột vỏ sau đó băm nhỏ. Hành lá lột vỏ và cắt bỏ phần rễ bẩn, rửa sạch rồi cắt nhỏ.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-3-so-che-nguyen-lieu-phu-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-008.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-009.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-010.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6734",
        "stepNumber": 4,
        "instructionText": "Khi thấy cháo vừa nở thì cho phần hạt sen đã sơ chế vào, đảo đều và nấu trong khoảng 20 phút với lửa nhỏ.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-4-cho-hat-sen-vao-nau-voi-chao-chao-thit-bo-h.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-012.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          }
        ]
      },
      {
        "stepId": "step-6735",
        "stepNumber": 5,
        "instructionText": "Ướp thịt bò đã băm với: 1/2 phần hành tỏi băm, 1 muỗng cà phê hạt nêm, 1/3 muỗng cà phê bột ngọt, 1/3 muỗng cà phê đường cát, trộn đều và ướp trong khoảng 15 - 20 phút để thịt thấm gia vị.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-5-uop-thit-bo-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-014.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          }
        ]
      },
      {
        "stepId": "step-6736",
        "stepNumber": 6,
        "instructionText": "Nêm vào cháo: 1 muỗng canh hạt nêm, 1/2 muỗng cà phê bột ngọt, 1 muỗng canh đường phèn, đảo đều và nêm nếm lại gia vị cho vừa ăn.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-6-nem-gia-vi-vao-chao-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-016.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          }
        ]
      },
      {
        "stepId": "step-6737",
        "stepNumber": 7,
        "instructionText": "Bắc chảo lên bếp với 2 muỗng canh dầu ăn, dầu nóng thì cho phần hành tỏi băm còn lại vào phi thơm.\nTiếp theo cho thịt bò đã ướp vào xào với lửa lớn đến khi thấy thịt bò săn và tái lại thì tắt bếp.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-7-xao-thit-bo-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-018.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "images/image-019.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-020.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6738",
        "stepNumber": 8,
        "instructionText": "Cháo hạt sen đã chín thì bạn chuyển cháo vào nồi đất rồi phần thịt bò vừa xào và 1 cái lòng đỏ trứng gà vào, đảo nhẹ tay để giữ nguyên được phần lòng đỏ.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-8-nau-chao-voi-thit-bo-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-022.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-023.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-024.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6739",
        "stepNumber": 9,
        "instructionText": "Tiếp đến bạn cho hành lá cắt nhỏ vào nồi và trang trí thêm 1 ít thịt bò xào lên trên mặt.\nCuối cùng bạn thêm 1 ít tiêu xay vào và trộn đều lên nữa là có thể thưởng thức được rồi.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-9-hoan-thanh-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-026.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          }
        ]
      },
      {
        "stepId": "step-6740",
        "stepNumber": 10,
        "instructionText": "Món cháo thịt bò hạt sen hoàn thành nổi bật với màu vàng - xanh đẹp mắt cùng hương thơm nức mũi cực hấp dẫn.\nHạt cháo nở bung đều mang vị béo ngậy từ lòng đỏ trứng gà, mềm ngọt của thịt bò kết hợp với phần hạt sen chín mềm mà không bị nát có vị bùi béo ngon miệng.\nMón cháo chẳng những thơm ngon, bổ dưỡng mà còn có tác dụng giải cảm hiệu quả, chắc chắn sẽ là lựa chọn hoàn hảo cho bữa ăn gia đình bạn đấy!\nThế là mình đã giúp bạn bỏ túi được cách nấu cháo thịt bò hạt sen hấp dẫn, đầy bổ dưỡng với những bước làm vô cùng đơn giản. Nhanh tay vào bếp trổ tài ngay nhé!",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/buoc-10-thanh-pham-chao-thit-bo-hat-sen.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-028.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-029.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-thit-bo-hat-sen-don-gian-thom-ngon-va-giup-giai-nhiet/images/image-030.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      }
    ]
  },
  {
    "recipeId": "7",
    "statusAtLoad": "PUBLISHED",
    "title": "Cách nấu cháo Quảng thơm ngon chuẩn vị người Hoa kinh doanh tại nhà cực dễ",
    "baseServings": 4,
    "loadedAt": 1731300000000,
    "ingredients": [
      {
        "rowId": "ing-10845",
        "ingredientId": "863",
        "name": "Gạo dẻo",
        "quantity": 500,
        "amountText": "500 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 1
      },
      {
        "rowId": "ing-10846",
        "ingredientId": "1628",
        "name": "Nếp",
        "quantity": 50,
        "amountText": "50 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 2
      },
      {
        "rowId": "ing-10847",
        "ingredientId": "2711",
        "name": "Xương ống heo",
        "quantity": 1.5,
        "amountText": "1.5 kg",
        "unitName": "kg",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 3
      },
      {
        "rowId": "ing-10848",
        "ingredientId": "859",
        "name": "Gan heo",
        "quantity": 400,
        "amountText": "400 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 4
      },
      {
        "rowId": "ing-10849",
        "ingredientId": "426",
        "name": "Cật heo",
        "quantity": 1,
        "amountText": "1 cái",
        "unitName": "cái",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 5
      },
      {
        "rowId": "ing-10850",
        "ingredientId": "62",
        "name": "Bao tử",
        "quantity": 1,
        "amountText": "1 cái",
        "unitName": "cái",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 6
      },
      {
        "rowId": "ing-10851",
        "ingredientId": "2478",
        "name": "Tim heo",
        "quantity": 1,
        "amountText": "1 cái",
        "unitName": "cái",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 7
      },
      {
        "rowId": "ing-10852",
        "ingredientId": "2387",
        "name": "Thịt heo xay",
        "quantity": 300,
        "amountText": "300 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 8
      },
      {
        "rowId": "ing-10853",
        "ingredientId": "974",
        "name": "Gừng/ hành lá/ hành tím/ hành ngò/ xà lách/ giá",
        "quantity": null,
        "amountText": "1 ít",
        "unitName": "vừa đủ",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 9
      },
      {
        "rowId": "ing-10854",
        "ingredientId": "21",
        "name": "Baking soda",
        "quantity": 0.5,
        "amountText": "1/2 muỗng canh",
        "unitName": "muỗng canh",
        "note": "muối nở",
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 10
      },
      {
        "rowId": "ing-10855",
        "ingredientId": "216",
        "name": "Bột năng",
        "quantity": null,
        "amountText": "1 ít",
        "unitName": "vừa đủ",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 11
      },
      {
        "rowId": "ing-10856",
        "ingredientId": "904",
        "name": "Giấm/ rượu nếp",
        "quantity": null,
        "amountText": "1 ít",
        "unitName": "vừa đủ",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 12
      },
      {
        "rowId": "ing-10857",
        "ingredientId": "938",
        "name": "Gia vị thông dụng",
        "quantity": null,
        "amountText": "1 ít",
        "unitName": "vừa đủ",
        "note": "muối/ đường/ bột nêm/ bột ngọt/ tiêu/ nước mắm...",
        "groupName": "Nguyên liệu (Cho 4 - 6 người)",
        "isOptional": false,
        "position": 13
      }
    ],
    "steps": [
      {
        "stepId": "step-6648",
        "stepNumber": 1,
        "instructionText": "Sơ chế xương ống: Cho 1/2 muỗng canh muối vào thau nước, khuấy đều rồi ngâm 1.5kg xương ống heo khoảng 30 phút. Sau đó rửa sạch lại.\nSơ chế bao tử heo: Rửa sơ bên trong bao tử, lật ra ngoài và chà sạch với 2 muỗng canh muối. Rửa lại nước. Tiếp tục cho 2 muỗng canh muối, 3 muỗng canh bột năng và một ít giấm vào, chà kỹ để loại bỏ nhớt và mùi hôi. Rửa sạch lại bằng nước.\nTrụng sơ lòng heo: Đun sôi một chảo nước, cho 50ml rượu nếp vào để khử mùi. Trụng bao tử mỗi mặt khoảng 2 phút, vớt ra ngâm ngay vào thau nước lạnh để bao tử không bị đen. Trụng xương ống và tim heo trong nước mới (không dùng lại nước trụng bao tử), vớt ra rửa sạch xương ống.\nLàm sạch bao tử sau trụng: Dùng dao cạo sạch phần màu vàng và mỡ heo còn sót lại trên bao tử. Rửa sạch lại bằng nước. Bao tử sẽ trắng và không còn nhớt, mùi hôi.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/buoc-1-so-che-xuong-ong-va-cac-loai-long-heo-chao-.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-002.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-003.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-004.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6649",
        "stepNumber": 2,
        "instructionText": "Cho 5 lít nước vào nồi lớn. Cho xương ống và tim heo đã sơ chế vào nấu.\nSơ chế hành tím và gừng: Lột vỏ 100gr hành tím và nướng 30gr gừng cho thơm. Phi thơm hành tím, đập dập gừng.\nKhi nước sôi, vớt bọt sạch sẽ. Cho hành tím đã phi và gừng đã đập dập vào. Cho bao tử đã làm sạch vào nồi, nấu từ 45 đến 60 phút cho mềm.\nMẹo: Mách nhỏ:Để nước lèo trong và ngọt, nhớ vớt bọt thường xuyên và không dùng lại nước trụng lòng heo.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/buoc-2-nau-nuoc-leo-va-ham-long-heo-chao-quang.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-006.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-007.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          }
        ]
      },
      {
        "stepId": "step-6650",
        "stepNumber": 3,
        "instructionText": "Sơ chế cật heo: Cắt cật heo thành từng lát mỏng, loại bỏ hoàn toàn phần gân trắng bên trong để cháo không bị đỏ và cật giòn ngon. Cắt xong, cho vào tô với 1 muỗng cà phê muối và 1/2 muỗng canh bột năng, trộn đều rồi rửa sạch.\nNgâm cật đã rửa sạch vào thau nước đá khoảng 20-30 phút để cật nở to, giòn hơn.\nSơ chế gan heo: Cắt 400gr gan heo thành miếng mỏng. Rửa lại một nước. Cho gan vào tô với 400ml nước và 1/2 muỗng canh bột baking soda (muối nở), ngâm 20 phút để gan mềm bùi. Rửa thật sạch lại dưới vòi nước.\nƯớp gan heo: Để gan ráo nước hoặc dùng giấy bếp thấm khô. Cho gan vào tô, thêm 10gr gừng và 10gr đầu hành giã nhuyễn, 1/2 muỗng canh bột năng, 1/2 muỗng cà phê muối, 1 muỗng cà phê đường, 1/2 muỗng cà phê bột nêm gà. Trộn đều và cho vào tủ lạnh.\nMẹo: Mách nhỏ:Khi cắt cật, dùng dao thật bén và cắt dứt khoát để miếng cật đẹp, không bị răng cưa. Có thể lót khăn dưới thớt để dễ cắt hơn.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/buoc-3-so-che-cat-heo-va-gan-heo-chao-quang.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-009.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-010.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-011.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6651",
        "stepNumber": 4,
        "instructionText": "Vo sạch 500gr gạo dẻo và 50gr nếp.\nCho gạo nếp đã vo vào nồi đất (hoặc nồi có đáy dày) với 3 lít nước. Nấu trên lửa vừa.\nNấu khoảng 25 phút cho cháo đặc lại. Tắt bếp.\nMẹo: Mách nhỏ:Người Hoa thường dùng gạo dẻo và thêm nếp để cháo có độ sánh, dẻo mịn và ít ra nước. Khi cháo sắc xuống, chỉ thêm nước sôi, không thêm nước lạnh để tránh cháo bị ra nước.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/buoc-4-nau-chao-dac-chao-quang.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-013.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-014.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-015.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6652",
        "stepNumber": 5,
        "instructionText": "Khi xương ống, tim heo và bao tử trong nồi nước lèo đã mềm, vớt ra.\nNgâm lòng heo và xương: Chuẩn bị thau 3 lít nước với 30gr muối, khuấy tan. Ngâm tim heo, bao tử và xương ống vào để nguội, giúp thấm vị và ngon hơn.\nVớt sạch hành tím trong nồi nước lèo, thêm nước vào tùy theo lượng nước lèo mong muốn. Đun sôi lại.\nLàm heo viên: Cho 300gr thịt heo xay vào tô lớn. Thêm 1/2 muỗng cà phê bột năng, 1 muỗng cà phê bột nêm gà, 1 muỗng cà phê muối, 1 muỗng cà phê đường cát, 1/4 muỗng cà phê bột ngọt (tùy chọn), 1/8 muỗng cà phê tiêu và một ít dầu mè. Trộn đều gia vị.\nDùng tay nhồi và quết thịt, sau đó đập vài cái cho thịt dai. Vo thịt thành từng viên tròn vừa ăn.\nKhi nồi nước lèo sôi, múc một vá nước lèo xối lên đĩa heo viên để không bị dính. Cho heo viên vào nấu khoảng 2-3 phút cho nổi lên là chín. Vớt ra ngâm vào thau nước muối đã ngâm lòng heo.\nMẹo: Mách nhỏ:Heo viên cần nêm hơi đậm đà vì sẽ nấu 2 lần.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/buoc-5-hoan-thanh-nuoc-leo-va-lam-heo-vien-chao-qu.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-017.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-018.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-019.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6653",
        "stepNumber": 6,
        "instructionText": "Pha 2 lít cháo từ cháo đặc và nước lèo theo độ đặc lỏng mong muốn.\nNêm cháo với 10gr muối (muối nhạt hơn muối i-ốt), 1 muỗng cà phê nước mắm 40 độ đạm, 1 muỗng cà phê đường và 1 muỗng cà phê bột nêm gà. Khuấy đều.\nĐun sôi lại nồi cháo đã nêm. Tắt bếp.\nSơ chế ăn kèm: Gọt vỏ gừng, cắt sợi thật nhuyễn. Cắt tim heo và bao tử thành miếng mỏng hoặc khoảng 1cm. Cật heo sau khi ngâm đá thì rửa sạch lại.\nChuẩn bị xà lách hoặc giá cắt nhuyễn, hành ngò cắt nhỏ, ớt xay, dầu mè.\nMẹo: Mách nhỏ:Nếu nấu để bán, nêm hơi nhạt để trừ hao độ mặn khi nấu từng tô với topping. Nếu nấu cho gia đình, có thể nêm vừa ăn ngay từ đầu.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/buoc-6-nem-nem-chao-va-chuan-bi-an-kem-chao-quang.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-021.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-022.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-023.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6654",
        "stepNumber": 7,
        "instructionText": "Khi bán cháo, dùng nồi nhỏ chuyên dụng (như nồi có cán bằng gỗ) để nấu từng tô. Múc khoảng 2 vá cháo (khoảng 700ml) vào nồi.\nBật lửa lớn, cho một ít gừng sợi vào nấu cùng cho thơm. Khi cháo sôi, cho các loại thập cẩm (gan heo, cật heo, tim heo, bao tử, heo viên) vào nấu.\nLiên tục khuấy đều để thập cẩm chín đều và cháo không bị khét. Nấu đến khi gan và cật không còn ra máu là chín. Đổ cháo ra tô đã lót sẵn xà lách hoặc giá. Rắc thêm hành ngò, gừng sợi, tiêu. Rưới một ít dầu mè và thêm ớt xay nếu thích.\nCháo Quảng thập cẩm kiểu Hoa nóng hổi, sánh mịn, gan mềm bùi, lòng heo giòn dai, thơm lừng vị gừng và dầu mè sẽ làm hài lòng cả những thực khách khó tính nhất.\nHy vọng với những kinh nghiệm chia sẻ chi tiết này, bạn sẽ tự tin thực hiện món cháo Quảng để chiêu đãi người thân hoặc bắt đầu kế hoạch kinh doanh của mình!",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/buoc-7-thanh-pham-chao-quang.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-025.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-quang-thom-ngon-chuan-vi-nguoi-hoa-kinh-doanh-tai-nha-cuc-de/images/image-026.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          }
        ]
      }
    ]
  },
  {
    "recipeId": "9",
    "statusAtLoad": "PUBLISHED",
    "title": "Cách nấu cháo vịt miền Tây ăn cùng gỏi bắp cải chua ngọt cực ngon tại nhà",
    "baseServings": 4,
    "loadedAt": 1731300000000,
    "ingredients": [
      {
        "rowId": "ing-11056",
        "ingredientId": "2650",
        "name": "Vịt",
        "quantity": 1,
        "amountText": "1 con",
        "unitName": "con",
        "note": "khoảng 1.4kg",
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 1
      },
      {
        "rowId": "ing-11057",
        "ingredientId": "862",
        "name": "Gạo",
        "quantity": 160,
        "amountText": "160 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 2
      },
      {
        "rowId": "ing-11058",
        "ingredientId": "2",
        "name": "Cà rốt",
        "quantity": 200,
        "amountText": "200 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 3
      },
      {
        "rowId": "ing-11059",
        "ingredientId": "1620",
        "name": "Nấm rơm",
        "quantity": 300,
        "amountText": "300 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 4
      },
      {
        "rowId": "ing-11060",
        "ingredientId": "71",
        "name": "Bắp chuối bào",
        "quantity": 300,
        "amountText": "300 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 5
      },
      {
        "rowId": "ing-11061",
        "ingredientId": "69",
        "name": "Bắp cải trắng",
        "quantity": 500,
        "amountText": "500 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 6
      },
      {
        "rowId": "ing-11062",
        "ingredientId": "968",
        "name": "Gừng",
        "quantity": 50,
        "amountText": "50 gr",
        "unitName": "g",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 7
      },
      {
        "rowId": "ing-11063",
        "ingredientId": "1017",
        "name": "Hành lá/ ngò rí/ rau thơm",
        "quantity": null,
        "amountText": "1 ít",
        "unitName": "vừa đủ",
        "note": "ăn kèm gỏi",
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 8
      },
      {
        "rowId": "ing-11064",
        "ingredientId": "1039",
        "name": "Hành phi",
        "quantity": null,
        "amountText": "1 ít",
        "unitName": "vừa đủ",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 9
      },
      {
        "rowId": "ing-11065",
        "ingredientId": "2078",
        "name": "Rượu gừng",
        "quantity": 3,
        "amountText": "3 muỗng canh",
        "unitName": "muỗng canh",
        "note": "hoặc rượu trắng/gừng tươi giã nhuyễn",
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 10
      },
      {
        "rowId": "ing-11066",
        "ingredientId": "1851",
        "name": "Ớt băm",
        "quantity": 1,
        "amountText": "1 muỗng canh",
        "unitName": "muỗng canh",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 11
      },
      {
        "rowId": "ing-11067",
        "ingredientId": "1704",
        "name": "Nước cốt chanh",
        "quantity": 6,
        "amountText": "6 muỗng canh",
        "unitName": "muỗng canh",
        "note": null,
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 12
      },
      {
        "rowId": "ing-11068",
        "ingredientId": "938",
        "name": "Gia vị thông dụng",
        "quantity": null,
        "amountText": "1 ít",
        "unitName": "vừa đủ",
        "note": "muối/ đường/ bột ngọt/ hạt nêm/ nước mắm/ tiêu xay",
        "groupName": "Nguyên liệu (Cho 4 - 5 người)",
        "isOptional": false,
        "position": 13
      }
    ],
    "steps": [
      {
        "stepId": "step-6781",
        "stepNumber": 1,
        "instructionText": "Dùng khoảng 3 muỗng canh rượu gừng (hoặc rượu trắng pha gừng tươi giã nhuyễn) chà xát kỹ lên toàn bộ da vịt. Sau đó, dội nước ấm lên và ngâm khoảng 10 phút. Rửa sạch vịt lại lần nữa với nước lạnh.\nVo sạch 160gr gạo hằng ngày (không cần ngâm). Trộn vào gạo nửa muỗng cà phê muối để hạt gạo săn chắc hơn khi nấu cháo.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/buoc-1-so-che-vit-va-gao-nau-chao-vit-mien-tay-cha.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-002.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-003.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          }
        ]
      },
      {
        "stepId": "step-6782",
        "stepNumber": 2,
        "instructionText": "Nấu một nồi nước khoảng 3 lít, khi nước hơi ấm thì cho vịt vào. Thêm 1 muỗng canh muối vào nước luộc. Để lửa lớn cho nước sôi rồi hạ lửa vừa, luộc vịt khoảng 30 phút. Lưu ý, trong khi luộc, cứ 10 phút nên trở mặt vịt một lần để vịt chín đều.\nDùng đũa đâm vào phần đùi vịt, nếu không thấy nước đỏ chảy ra là vịt đã chín. Vớt vịt ra để nguội hoàn toàn trước khi chặt. Cho gạo đã sơ chế vào nồi nước luộc vịt vừa vớt ra, nấu lửa vừa cho hạt gạo nở từ từ.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/buoc-2-luoc-thit-vit-va-nau-noi-chao-vit-chao-vit-.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-005.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-006.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-007.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6783",
        "stepNumber": 3,
        "instructionText": "Chuẩn bị 200gr cà rốt. Một nửa xắt hạt lựu để cho vào cháo, một nửa bào sợi để làm gỏi. 300gr nấm rơm rửa sạch. Hành lá và ngò rí rửa sạch, cắt nhỏ, phần đầu hành để riêng.\nKhi cháo đã sôi và hạt gạo bắt đầu nở, cho cà rốt xắt hạt lựu, nấm rơm và đầu hành vào nồi cháo. Nêm gia vị cho cháo với 1 muỗng cà phê bột ngọt, 1 muỗng canh đường, 1/4 muỗng cà phê hạt nêm, 1 muỗng cà phê nước mắm.\nKhi cháo chín nhừ, cho hành lá và ngò rí đã cắt nhỏ vào rồi tắt bếp.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/buoc-3-so-che-rau-cu-va-nem-chao-chao-vit-mien-tay.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          }
        ]
      },
      {
        "stepId": "step-6784",
        "stepNumber": 4,
        "instructionText": "Các nguyên liệu làm gỏi gồm 300gr bắp chuối bào, 500gr bắp cải trắng, cà rốt bào sợi (phần còn lại) và rau thơm các loại. Rửa sạch và để ráo.\nTrong một cái chén, trộn đều 50gr gừng giã nhuyễn, 1 muỗng canh ớt băm, 6 muỗng canh nước cốt chanh, 4 muỗng canh đường và 4 muỗng canh nước mắm.\nTrộn 2 muỗng canh đường, 2 muỗng canh nước mắm, gừng giã và ớt băm. Bạn có thể điều chỉnh độ chua cay theo khẩu vị gia đình.\nCho các loại rau làm gỏi vào một cái thau lớn, rưới nước mắm trộn gỏi đã pha vào và bóp nhẹ tay cho thấm đều gia vị.",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/buoc-4-lam-goi-vit-mien-tay-va-nuoc-cham-chao-vit-.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-010.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-011.jpg",
            "alt": "Ảnh minh họa",
            "position": 3
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-012.jpg",
            "alt": "Ảnh minh họa",
            "position": 4
          }
        ]
      },
      {
        "stepId": "step-6785",
        "stepNumber": 5,
        "instructionText": "Thịt vịt đã nguội chặt (hoặc dùng kéo cắt) thành miếng vừa ăn. Xếp gỏi ra đĩa lớn, sau đó đặt thịt vịt đã chặt lên trên, rắc thêm một ít hành phi thơm.\nMúc cháo ra tô, rắc thêm tiêu xay và hành phi để tăng thêm hương vị hấp dẫn. Thưởng thức món cháo gỏi vịt miền Tây nóng hổi, đậm đà cùng với nước mắm gừng cay nồng.\nVậy là món cháo vịt miền tây chuẩn vị đã hoàn thành! Với công thức chi tiết này, bạn hoàn toàn có thể tự tay chiêu đãi gia đình món ăn đậm đà hương vị ẩm thực miền Tây. Hãy thử ngay và chia sẻ thành quả của bạn nhé, chắc chắn ai cũng sẽ tấm tắc khen ngon!",
        "timerSeconds": null,
        "images": [
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/buoc-5-thanh-pham-chao-vit-mien-tay.jpg",
            "alt": "Ảnh minh họa",
            "position": 1
          },
          {
            "url": "https://cdn.angingon.com/recipes/cach-nau-chao-vit-mien-tay-an-cung-goi-bap-cai-chua-ngot-cuc-ngon-tai-nha/images/image-014.jpg",
            "alt": "Ảnh minh họa",
            "position": 2
          }
        ]
      }
    ]
  }
];
