import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { searchApi } from '../api/searchApi';

interface Props {
  slug: string;
  name: string;
  /** Background colour while there is no photo (wk-photo tone). */
  tone: 'warm' | 'olive' | 'saffron' | 'cream';
}

/** A category of the home page: the photo of its first recipe behind the name; links to the search of that category. */
export function CategoryTile({ slug, name, tone }: Props) {
  const [photo, setPhoto] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    searchApi
      .searchRecipes({ category: [slug] })
      .then((res) => {
        if (active) setPhoto(res.items.find((r) => r.coverImageUrl)?.coverImageUrl ?? null);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, [slug]);

  return (
    <Link to={`/search?category=${slug}`} className={`wx-home-cat wk-photo--${tone}`}>
      {photo && !failed && <img src={photo} alt="" loading="lazy" onError={() => setFailed(true)} />}
      <span className="wx-home-cat__name">{name}</span>
    </Link>
  );
}
