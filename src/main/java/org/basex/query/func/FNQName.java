package org.basex.query.func;

import static org.basex.query.QueryText.*;
import static org.basex.query.util.Err.*;
import static org.basex.util.Token.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * QName functions.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class FNQName extends StandardFunc {
  /**
   * Constructor.
   * @param ii input info
   * @param f function definition
   * @param e arguments
   */
  public FNQName(final InputInfo ii, final Function f, final Expr... e) {
    super(ii, f, e);
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    switch(sig) {
      case IN_SCOPE_PREFIXES: return inscope(ctx);
      default:                return super.iter(ctx);
    }
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    // functions have 1 or 2 arguments...
    final Item it = expr[0].item(ctx, info);
    final Item it2 = expr.length == 2 ? expr[1].item(ctx, info) : null;
    switch(sig) {
      case RESOLVE_QNAME:            return resolveQName(ctx, it, it2);
      case QNAME:                    return qName(it, it2);
      case LOCAL_NAME_FROM_QNAME:    return lnFromQName(ctx, it);
      case PREFIX_FROM_QNAME:        return prefixFromQName(ctx, it);
      case NAMESPACE_URI_FOR_PREFIX: return nsUriForPrefix(it, it2);
      case RESOLVE_URI:              return resolveURI(ctx, it, it2);
      default:                       return super.item(ctx, ii);
    }
  }

  /**
   * Returns the in-scope prefixes of the specified node.
   * @param ctx query context
   * @return prefix sequence
   * @throws QueryException query exception
   */
  private Iter inscope(final QueryContext ctx) throws QueryException {
    final ANode node = (ANode) checkType(expr[0].item(ctx, info),
        NodeType.ELM);

    final Atts ns = node.nsScope().add(XML, XMLURI);
    final int as = ns.size();
    final ValueBuilder vb = new ValueBuilder(as);
    for(int a = 0; a < as; ++a) {
      final byte[] key = ns.name(a);
      if(key.length + ns.string(a).length != 0) vb.add(Str.get(key));
    }
    return vb;
  }

  /**
   * Resolves a QName.
   * @param it qname
   * @param it2 item
   * @return prefix sequence
   * @throws QueryException query exception
   */
  private Item qName(final Item it, final Item it2) throws QueryException {
    final byte[] uri = checkEStr(it);
    final byte[] name = checkEStr(it2);
    final byte[] str = !contains(name, ':') && eq(uri, XMLURI) ?
        concat(XMLC, name) : name;
    if(!XMLToken.isQName(str)) Err.value(info, AtomType.QNM, Str.get(name));
    final QNm nm = new QNm(str, uri);
    if(nm.hasPrefix() && uri.length == 0)
      Err.value(info, AtomType.URI, Str.get(nm.uri()));
    return nm;
  }

  /**
   * Returns the local name of a QName.
   * @param ctx query context
   * @param it qname
   * @return prefix sequence
   * @throws QueryException query exception
   */
  private Item lnFromQName(final QueryContext ctx, final Item it) throws QueryException {
    if(it == null) return null;
    final QNm nm = (QNm) checkType(it, AtomType.QNM);
    return AtomType.NCN.cast(Str.get(nm.local()), ctx, info);
  }

  /**
   * Returns the local name of a QName.
   * @param ctx query context
   * @param it qname
   * @return prefix sequence
   * @throws QueryException query exception
   */
  private Item prefixFromQName(final QueryContext ctx, final Item it)
      throws QueryException {

    if(it == null) return null;
    final QNm nm = (QNm) checkType(it, AtomType.QNM);
    return nm.hasPrefix() ?
        AtomType.NCN.cast(Str.get(nm.prefix()), ctx, info) : null;
  }

  /**
   * Returns a new QName.
   * @param ctx query context
   * @param it qname
   * @param it2 item
   * @return prefix sequence
   * @throws QueryException query exception
   */
  private Item resolveQName(final QueryContext ctx, final Item it,
      final Item it2) throws QueryException {

    final ANode base = (ANode) checkType(it2, NodeType.ELM);
    if(it == null) return null;

    final byte[] name = checkEStr(it);
    if(!XMLToken.isQName(name)) Err.value(info, AtomType.QNM, it);

    final QNm nm = new QNm(name);
    final byte[] pref = nm.prefix();
    final byte[] uri = base.uri(pref, ctx);
    if(uri == null) NSDECL.thrw(info, pref);
    nm.uri(uri);
    return nm;
  }

  /**
   * Returns the namespace URI for a prefix.
   * @param it qname
   * @param it2 item
   * @return prefix sequence
   * @throws QueryException query exception
   */
  private Item nsUriForPrefix(final Item it, final Item it2) throws QueryException {
    final byte[] pref = checkEStr(it);
    final ANode an = (ANode) checkType(it2, NodeType.ELM);
    if(eq(pref, XML)) return Uri.uri(XMLURI, false);
    final Atts at = an.nsScope();
    final byte[] s = at.string(pref);
    return s == null || s.length == 0 ? null : Uri.uri(s, false);
  }

  /**
   * Resolves a URI.
   * @param ctx query context
   * @param it item
   * @param it2 second item
   * @return prefix sequence
   * @throws QueryException query exception
   */
  private Item resolveURI(final QueryContext ctx, final Item it, final Item it2)
      throws QueryException {
    if(it == null) return null;
    final Uri rel = Uri.uri(checkEStr(it));
    if(!rel.isValid()) URIINV.thrw(info, rel);
    if(rel.isAbsolute()) return rel;
    final Uri base = it2 == null ? ctx.sc.baseURI() : Uri.uri(checkEStr(it2));
    if(!base.isValid()) URIINV.thrw(info, base);
    if(!base.isAbsolute()) URIABS.thrw(info, base);
    return base.resolve(rel);
  }
}
