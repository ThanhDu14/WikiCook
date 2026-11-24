/** Attribution an imported recipe must show (scraper README): "Nguồn: angingon.com", opening the original page. */
export function SourceCredit({ source }: { source: { name: string; url: string } }) {
  return (
    <p className="wk-small wk-muted">
      Nguồn:{' '}
      <a className="wk-link" href={source.url} target="_blank" rel="noopener noreferrer">
        {source.name}
      </a>
    </p>
  );
}
