package com.dominator.gearly.architecture;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * <b>Architecture fitness functions.</b> These are what make the bounded-context structure
 * <i>strict</i> rather than aspirational: the layer rules from the DDD plan, expressed as
 * failing tests instead of as a convention people are asked to remember.
 *
 * <h2>Scoping, and the empty-match escape hatch</h2>
 * S8 wrote these rules in full against packages that were still empty, so every one of them
 * carried {@code allowEmptyShould(true)} — otherwise ArchUnit fails a rule that matches
 * nothing. That was the right call then and a liability afterwards: a rule matching zero
 * classes passes, so it proves nothing, and nothing would have told us.
 *
 * <p><b>S10 removed it.</b> The ordering context populates {@code domain},
 * {@code application}, {@code infrastructure} and {@code api}, so every rule below now has
 * real classes to check and says so by failing if it ever stops having them. A rule here can
 * no longer pass by vacuity.
 *
 * <p>Rules that would fail against the <em>legacy</em> packages
 * ({@code model}, {@code service}, {@code controller}, …) are still scoped to the new packages
 * with {@link #NEW_PACKAGES}. Those scopes come off in S13, at which point every rule here
 * applies repo-wide. Each such rule carries a {@code SCOPE:} note saying so.
 */
@AnalyzeClasses(
        packages = ArchitectureFitnessTest.ROOT,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureFitnessTest {

    static final String ROOT = "com.dominator.gearly";

    /** The bounded contexts. {@code shared} and {@code platform} are deliberately absent — see below. */
    private static final List<String> CONTEXTS = List.of(
            "ordering", "catalog", "cart", "reviews", "identity",
            "payments", "notification", "storage", "geo", "content", "assistant",
            "analytics");

    /**
     * Everything the DDD refactor owns: the contexts plus the shared kernel and the
     * cross-cutting platform package. Used to scope rules that the untouched legacy
     * packages would still fail. Removed in S13.
     */
    private static final String[] NEW_PACKAGES =
            Stream.concat(CONTEXTS.stream(), Stream.of("shared", "platform"))
                    .map(name -> ROOT + "." + name + "..")
                    .toArray(String[]::new);

    private static final String[] CONTEXT_PACKAGES =
            CONTEXTS.stream().map(name -> ROOT + "." + name + "..").toArray(String[]::new);

    /** The pre-refactor packages. Nothing inside a context's domain may reach back into these. */
    private static final String[] LEGACY_PACKAGES = {
            ROOT + ".ai..", ROOT + ".config..", ROOT + ".controller..", ROOT + ".dto..",
            ROOT + ".exception..", ROOT + ".mapper..", ROOT + ".model..",
            ROOT + ".repository..", ROOT + ".security..", ROOT + ".service..",
            ROOT + ".websocket.."
    };

    // ------------------------------------------------------------------------
    // Layer rules
    // ------------------------------------------------------------------------

    /**
     * The domain is plain Java. It must be constructible and assertable in a unit test with
     * no Spring context, no HTTP, and no database session — which is only true if none of
     * those types can be named from inside it.
     *
     * <p>Spring Data's {@code annotation} and {@code core.mapping} packages are the one
     * concession: the plan keeps aggregates as {@code @Document} classes so that the stored
     * shape never has to change. Repository types are still banned — a repository is a port
     * the domain <em>declares</em>, never a Spring interface it extends.
     */
    @ArchTest
    static final ArchRule domain_is_free_of_framework_types =
            noClasses().that().resideInAPackage(ROOT + "..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..",
                            "org.springframework.security..",
                            "org.springframework.http..",
                            "org.springframework.stereotype..",
                            "org.springframework.data.mongodb.repository..",
                            "org.springframework.data.domain..",
                            "jakarta.servlet..",
                            "jakarta.persistence..")
                    .because("the domain must be testable with a plain constructor call: "
                            + "no web, no security, no HTTP, no Spring Data repositories")
                    .allowEmptyShould(false);

    /**
     * A domain package must not reach back into the pre-refactor code. This is what stops a
     * sprint from ending with a half-migrated tree: if {@code ordering.domain} still needs
     * {@code model.Product}, the move is not finished.
     */
    @ArchTest
    static final ArchRule domain_does_not_reach_back_into_legacy_packages =
            noClasses().that().resideInAPackage(ROOT + "..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(LEGACY_PACKAGES)
                    .because("a migrated aggregate that still imports the old model is not migrated")
                    .allowEmptyShould(false);

    /**
     * The inbound HTTP edge talks to the application layer, never to an adapter. A
     * controller that injects a Spring Data repository has skipped the use case.
     */
    @ArchTest
    static final ArchRule api_does_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage(ROOT + "..api..")
                    .should().dependOnClassesThat().resideInAPackage(ROOT + "..infrastructure..")
                    .because("controllers depend on use cases, not on adapters")
                    .allowEmptyShould(false);

    /** Dependencies point inward: an adapter implements a port, a port never names an adapter. */
    @ArchTest
    static final ArchRule domain_does_not_depend_on_its_own_infrastructure =
            noClasses().that().resideInAPackage(ROOT + "..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            ROOT + "..infrastructure..", ROOT + "..api..", ROOT + "..application..")
                    .because("the dependency rule points inward — the domain is the innermost layer")
                    .allowEmptyShould(false);

    /**
     * The authenticated principal stops at the controller.
     *
     * <p>An application service that takes an {@code AuthenticatedUser} — a Spring Security
     * {@code UserDetails} — cannot be constructed in a test without a security context, and
     * has quietly made "who is calling" part of a use case's input rather than a decision the
     * edge already made. Controllers unwrap it into a {@code UserId} and pass that.
     *
     * <p>Both the framework's packages and this codebase's own are banned, because
     * {@code AuthenticatedUser} lives in one of them.
     *
     * <h2>S12: the codebase's half moved, and the rule got stricter</h2>
     * {@code com.dominator.gearly.security} is gone; the whole access boundary —
     * {@code AuthenticatedUser}, the JWT filter, the filter chain, the password hasher and the
     * token issuer — is {@code platform.security} now. The banned package changed to match.
     *
     * <p>What did <em>not</em> get an exception is {@code org.springframework.security.crypto}.
     * Identity needs password hashing, and the easy way to give it that would have been to carve
     * a crypto-shaped hole here. Instead the context declares a {@code PasswordHasher} port and
     * {@code platform.security.BCryptPasswordHasher} implements it, so this rule stays absolute:
     * <b>no class in any bounded context, at any layer below the controller, names a Spring
     * Security type.</b> A rule with one exception has two, eventually.
     */
    @ArchTest
    static final ArchRule security_types_stop_at_the_api_layer =
            noClasses().that().resideInAnyPackage(CONTEXT_PACKAGES)
                    .and().resideOutsideOfPackage(ROOT + "..api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.security..", ROOT + ".platform.security..")
                    .because("controllers unwrap the principal into a typed id; "
                            + "a use case never sees a security type")
                    .allowEmptyShould(false);

    /**
     * A Spring Data repository is an implementation detail of one adapter.
     *
     * <p>The domain declares a port ({@code OrderRepository}); {@code infrastructure}
     * implements it in terms of whatever the database offers. An application service holding a
     * {@code MongoRepository} has skipped the port and pinned the use case to MongoDB — which
     * is precisely the state {@code CustomerOrderService} was in before S10.
     */
    @ArchTest
    static final ArchRule spring_data_repositories_live_only_in_infrastructure =
            noClasses().that().resideInAnyPackage(CONTEXT_PACKAGES)
                    .and().resideOutsideOfPackage(ROOT + "..infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.data.mongodb.repository..",
                            "org.springframework.data.repository..")
                    .because("the domain declares a port and an adapter implements it; "
                            + "no other layer names a Spring Data repository")
                    .allowEmptyShould(false);

    // ------------------------------------------------------------------------
    // Persistence rules
    // ------------------------------------------------------------------------

    /**
     * A {@code @Document} class <em>is</em> an aggregate, and aggregates live in the domain.
     * Finding one in a DTO or a controller package means the persistence shape has leaked
     * onto the wire — which is exactly how the current cart accepts a client-supplied price.
     *
     * <p>SCOPE: the new packages only. The legacy {@code model/} package fails this today by
     * construction; the scope comes off in S13 once {@code model/} is empty.
     */
    @ArchTest
    static final ArchRule documents_live_only_in_a_domain_package =
            classes().that().areAnnotatedWith(Document.class)
                    .and().resideInAnyPackage(NEW_PACKAGES)
                    .should().resideInAPackage(ROOT + "..domain..")
                    .because("a persistence document is an aggregate, and aggregates live in the domain")
                    .allowEmptyShould(false);

    /**
     * An aggregate root with a public setter is not an aggregate root — every invariant it
     * claims to protect can be walked around one field at a time. State changes go through
     * named behavior ({@code order.transitionTo(...)}, {@code product.reserve(...)}).
     *
     * <p>This is the rule that fails the moment someone puts Lombok's {@code @Setter} or
     * {@code @Data} back on a domain class: those annotations are source-retention and
     * invisible to ArchUnit, but the public setters they generate are not.
     */
    @ArchTest
    static final ArchRule aggregates_expose_no_public_setters =
            noMethods().that().areDeclaredInClassesThat().resideInAPackage(ROOT + "..domain..")
                    .and().areDeclaredInClassesThat().areAnnotatedWith(Document.class)
                    .should().bePublic()
                    .andShould().haveNameMatching("set[A-Z].*")
                    .because("an aggregate changes state through named behavior, not field assignment "
                            + "(this also catches Lombok @Setter/@Data on a domain class)")
                    .allowEmptyShould(false);

    /**
     * {@code MongoTemplate} is for the query side and for repository adapters. Nobody else.
     * A domain, application or api class that names it has skipped a port.
     *
     * <p>S10 narrowed this: it used to permit {@code analytics} alone. That was too strict to
     * be right rather than usefully strict. A repository adapter in {@code ..infrastructure..}
     * is <em>by definition</em> the layer that knows the storage technology — it exists to
     * implement a domain port in terms of it — and the customer order search cannot be
     * expressed any other way: it is a dozen optional regex clauses OR'd together, which the
     * derived-query DSL cannot build. Under the old wording the only way to satisfy the rule
     * was to leave the criteria behind in the legacy {@code repository/} package, i.e. to
     * pass the rule by not completing the move. The exemption is by layer, not by class name,
     * so it cannot be borrowed by an application service.
     *
     * <p>SCOPE: the new packages only — {@code service.admin} still injects it today. That
     * scope comes off in S13, when analytics and the adapters are the last holders.
     */
    @ArchTest
    static final ArchRule mongo_template_is_reserved_for_analytics_and_adapters =
            noClasses().that().resideInAnyPackage(NEW_PACKAGES)
                    .and().resideOutsideOfPackage(ROOT + ".analytics..")
                    .and().resideOutsideOfPackage(ROOT + "..infrastructure..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.data.mongodb.core.MongoTemplate")
                    .because("raw Mongo access belongs to the read side and to repository adapters; "
                            + "everything else goes through a port")
                    .allowEmptyShould(false);

    /**
     * <b>An aggregate is never bound from a request body.</b>
     *
     * <p>This rule exists because the codebase had the bug it describes, in the place it does
     * the most damage. {@code CartController.add}, {@code CartController.merge} and
     * {@code GuestCartController.add} all declared {@code @RequestBody CartItem} — a
     * {@code @Document} — so a customer could post their own {@code price} and the server
     * stored it without ever consulting the catalog. The S8 suite pinned a $1,599 product sold
     * at $0.01.
     *
     * <p>The S11 fix was structural: a cart line can only be built from a
     * {@code CatalogSnapshot}, so there is nowhere for a submitted price to land. This rule is
     * what stops the structural fix from being quietly undone by someone adding one more
     * "convenient" endpoint that binds the entity directly, which is exactly how the original
     * three arrived.
     *
     * <p>Deliberately about {@code @RequestBody} rather than about {@code @Document} appearing
     * in a signature at all: returning an aggregate is a different question (the response DTOs
     * answer it), and one an ArchUnit rule would answer badly.
     *
     * <p>SCOPE: the new packages only, while {@code controller/} still exists. Off in S13.
     */
    @ArchTest
    static void aggregates_are_never_bound_from_a_request_body(JavaClasses classes) {
        methods().that().areDeclaredInClassesThat().resideInAnyPackage(NEW_PACKAGES)
                .should(takeNoRequestBodyThatIsAPersistenceDocument())
                .because("a request body a client controls must never be a stored document — "
                        + "that is how the cart came to accept a client-supplied price")
                .allowEmptyShould(false)
                .check(classes);
    }

    /**
     * <b>A published event carries only shared-kernel types.</b>
     *
     * <p>The companion to {@code contexts_touch_each_other_only_through_published_types}, and
     * the more useful half. That rule catches a leak at the <em>consumer</em> — which means it
     * only fires once somebody has written the consumer and discovered they cannot. This one
     * catches it at the event, where the decision is actually made.
     *
     * <p>S10's {@code OrderPlaced} carried {@code List<OrderLine>}, which was invisible while
     * the only listener lived in the ordering context. The moment S11 moved the stock decrement
     * to {@code catalog} and the cart clear to {@code cart}, the event turned out to be
     * publishing ordering's internal line type to two other contexts. It carries
     * {@code Map<ProductId, Quantity>} now — and this rule is why it cannot drift back.
     *
     * <p>What counts as carryable: the JDK, {@code shared.domain}, and enums from the event's
     * own context (an {@code OrderStatus} <em>is</em> part of ordering's published language).
     * Anything else is internal.
     */
    @ArchTest
    static void published_events_carry_only_shared_kernel_types(JavaClasses classes) {
        classes().that().areAssignableTo(DomainEvent.class)
                .and().doNotHaveModifier(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
                .and().areNotInterfaces()
                .should(carryOnlyPublishableTypes())
                .because("an event is a contract between contexts; a consumer must be able to "
                        + "read every field of it without importing the publisher's internals")
                .allowEmptyShould(false)
                .check(classes);
    }

    // ------------------------------------------------------------------------
    // Context-boundary rules
    // ------------------------------------------------------------------------

    /**
     * The rule the whole refactor exists to enforce. A context may reach another context
     * only through that context's <em>published language</em>:
     *
     * <ul>
     *   <li>a <b>port</b> — an interface in the other context's {@code domain} package
     *       ({@code PaymentGateway}, {@code NotificationSender}, {@code FileStorage});</li>
     *   <li>a <b>domain event</b> — any implementor of {@code shared.domain.DomainEvent} in
     *       that same package ({@code OrderPlaced}, {@code OrderCancelled});</li>
     *   <li>a <b>published value</b> — a {@code *Snapshot} (the Catalog ACL) or a typed
     *       {@code *Id}.</li>
     * </ul>
     *
     * Everything else is internal. In particular an application service, a repository
     * adapter or a controller of another context is never a legal target: that is the
     * distributed-monolith failure mode this structure is meant to prevent.
     *
     * <p>{@code shared} and {@code platform} are not contexts and are skipped — every
     * context may use {@code shared}, and {@code platform} may use everything.
     *
     * <h2>S13: a port's own vocabulary counts as published</h2>
     * The first S13 port to carry a value made this rule fail for a reason that was the rule's
     * fault rather than the design's. {@code PaymentGateway.verifyNotification} returns a
     * {@code GatewaySettlement} and throws a {@code GatewayNotificationRejectedException}; the
     * rule accepted the interface and rejected both, so {@code ordering} was allowed to
     * <em>hold</em> the port but not to <em>call</em> it.
     *
     * <p>That is incoherent. A port is a contract, and a contract a caller cannot name the terms
     * of is not usable. The published set now includes, structurally:
     * <ul>
     *   <li>every type appearing in a port's method signatures — parameters, return types and
     *       their type arguments — because those are exactly what a caller must name;</li>
     *   <li>domain exceptions, i.e. types in {@code ..domain..} extending one of the shared
     *       kernel's four bases, because an unchecked exception a port raises is part of what it
     *       promises and the alternative is catching {@code RuntimeException}.</li>
     * </ul>
     *
     * <p>This is derived from the ports themselves rather than from a naming convention, which
     * is the same correction S11 made when it stopped recognizing events by an {@code …Event}
     * suffix. It cannot be borrowed: a type becomes published by being in a port's signature,
     * so publishing something is a deliberate edit to an interface, visible in review, and
     * withdrawing it from the port withdraws it here.
     */
    @ArchTest
    static void contexts_touch_each_other_only_through_published_types(JavaClasses classes) {
        // Phrased positively on purpose. `noClasses().should(customCondition)` inverts the
        // condition's events, which would silently make this rule inert.
        classes().that().resideInAnyPackage(CONTEXT_PACKAGES)
                .should(onlyTouchOtherContextsThroughPublishedTypes(typesNamedByPorts(classes)))
                .because("cross-context coupling goes through a port, an event, or a published value")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * The dependency between a context and the platform is one-way. Platform wires
     * everything; a context that needs something from the wiring has misplaced a rule.
     *
     * <h2>The one seam, and why it is the api layer</h2>
     * A controller has to name {@code platform.security.AuthenticatedUser}: that is the type
     * {@code @AuthenticationPrincipal} injects, and unwrapping it into a {@link UserId} is
     * precisely the job the inbound edge exists to do. So {@code ..api..} may reach
     * {@code platform.security} and nothing else may reach anything of the platform's.
     *
     * <p>The exemption is by <em>layer</em> and by <em>package</em>, not by class name, so it
     * cannot be borrowed: an application service cannot take an {@code AuthenticatedUser}
     * (it is not in {@code ..api..}), and a controller cannot reach into
     * {@code platform.config} or {@code platform.exception} (they are not
     * {@code platform.security}). Together with
     * {@link #security_types_stop_at_the_api_layer} this pins the principal to exactly one
     * layer of one package — which is stricter than S8's arrangement, where
     * {@code com.dominator.gearly.security} sat outside the platform entirely and this rule
     * never saw it.
     */
    @ArchTest
    static final ArchRule contexts_do_not_depend_on_the_platform =
            noClasses().that().resideInAnyPackage(CONTEXT_PACKAGES)
                    .and().resideOutsideOfPackage(ROOT + "..api..")
                    .should().dependOnClassesThat().resideInAPackage(ROOT + ".platform..")
                    .because("platform knows about the contexts; the contexts do not know about it")
                    .allowEmptyShould(false);

    /**
     * The companion to the rule above: an {@code ..api..} class may reach the platform's
     * security package, and no other part of the platform.
     *
     * <p>Stated separately because ArchUnit cannot express "all of X except Y in layer Z" in one
     * rule without the exemption silently widening. Two narrow rules that each fail loudly beat
     * one broad rule with a hole in it.
     */
    @ArchTest
    static final ArchRule api_reaches_only_the_platforms_security_package =
            noClasses().that().resideInAnyPackage(CONTEXT_PACKAGES)
                    .and().resideInAPackage(ROOT + "..api..")
                    .should().dependOnClassesThat(
                            JavaClass.Predicates.resideInAPackage(ROOT + ".platform..")
                                    .and(JavaClass.Predicates.resideOutsideOfPackage(
                                            ROOT + ".platform.security..")))
                    .because("a controller unwraps the security principal and needs nothing else "
                            + "the platform has")
                    .allowEmptyShould(true);

    /**
     * <b>Every admin route carries {@code @PreAuthorize}, not only the URL rule.</b>
     *
     * <p>{@code SecurityConfig} locks {@code /api/admin/**}, and that is the primary guard. It
     * is also a prefix match on a string held in one file, a long way from the handler it
     * protects — and every endpoint that has ever escaped such a rule did so by being mounted
     * somewhere the pattern did not reach: a controller moved package, a mapping edited, a new
     * method added under a path that merely looked similar. S12 put the annotation on all seven
     * admin controllers so the guarantee travels with the code; this rule is what stops the
     * eighth from shipping without it.
     *
     * <p>Class-level or method-level both satisfy it. {@code CategoryController} is why the rule
     * has to accept the method form: it serves {@code /api/categories} to anonymous shoppers and
     * {@code /api/admin/categories} to the console from one class, so a class-level annotation
     * would take the storefront's category menu away. {@code AdminMethodSecurityTest} is the
     * companion that proves the annotations actually refuse a caller rather than merely being
     * present — this rule checks presence, and presence alone is not a security property.
     *
     * <p>SCOPE: repo-wide, deliberately. {@code controller/admin} is legacy but its routes are
     * exactly as exposed as the new ones.
     */
    @ArchTest
    static void admin_routes_carry_method_level_authorization(JavaClasses classes) {
        classes().that(areMappedUnderTheAdminPrefix())
                .should(requireTheAdminRoleOnEveryAdminHandler())
                .because("a URL prefix rule in one file is not where an endpoint's authorization "
                        + "should live alone")
                .allowEmptyShould(false)
                .check(classes);
    }

    /** The shared kernel is upstream of everything, so it may know nothing about anything. */
    @ArchTest
    static final ArchRule shared_kernel_depends_on_no_context =
            noClasses().that().resideInAPackage(ROOT + ".shared..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            Stream.concat(Stream.of(CONTEXT_PACKAGES), Stream.of(ROOT + ".platform.."))
                                    .toArray(String[]::new))
                    .because("a shared kernel that depends on a context is not shared")
                    .allowEmptyShould(false);

    // ------------------------------------------------------------------------
    // The published-language condition
    // ------------------------------------------------------------------------

    /**
     * No parameter annotated {@code @RequestBody} may be — or contain — a {@code @Document}.
     * Containers are unwrapped, so {@code List<CartItem>} is caught as surely as
     * {@code CartItem}: the merge endpoint bound exactly that.
     */
    private static ArchCondition<JavaMethod> takeNoRequestBodyThatIsAPersistenceDocument() {
        return new ArchCondition<>("take no @RequestBody that is a persistence document") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter parameter : method.getParameters()) {
                    if (!parameter.isAnnotatedWith(RequestBody.class)) {
                        continue;
                    }
                    for (JavaClass carried : typesCarriedBy(parameter.getType())) {
                        if (carried.isAnnotatedWith(Document.class)) {
                            events.add(SimpleConditionEvent.violated(method, String.format(
                                    "%s binds '%s' from the request body, and it is a @Document "
                                            + "— a client would control its stored fields (%s)",
                                    method.getFullName(), carried.getSimpleName(),
                                    method.getSourceCodeLocation())));
                        }
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------------
    // The admin-authorization condition
    // ------------------------------------------------------------------------

    private static final String ADMIN_PREFIX = "/api/admin";

    /** Any controller with at least one mapping under {@code /api/admin}, at either level. */
    private static DescribedPredicate<JavaClass> areMappedUnderTheAdminPrefix() {
        return new DescribedPredicate<>("are mapped under " + ADMIN_PREFIX) {
            @Override
            public boolean test(JavaClass type) {
                if (mappedPathsOf(type).stream().anyMatch(path -> path.startsWith(ADMIN_PREFIX))) {
                    return true;
                }
                return type.getMethods().stream().anyMatch(ArchitectureFitnessTest::isAnAdminHandler);
            }
        };
    }

    private static ArchCondition<JavaClass> requireTheAdminRoleOnEveryAdminHandler() {
        return new ArchCondition<>("require the ADMIN role on every admin handler") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                if (type.isAnnotatedWith(PreAuthorize.class)) {
                    return;   // the whole controller is admin-only
                }
                for (JavaMethod handler : type.getMethods()) {
                    if (!isAnAdminHandler(handler) || handler.isAnnotatedWith(PreAuthorize.class)) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(type, String.format(
                            "%s answers under %s with no @PreAuthorize on the class or the "
                                    + "method — it is guarded only by the URL rule (%s)",
                            handler.getFullName(), ADMIN_PREFIX, handler.getSourceCodeLocation())));
                }
            }
        };
    }

    /**
     * A handler whose full path — the class mapping joined with the method's — reaches under
     * {@code /api/admin}. Both halves matter: {@code AdminOrderController} puts the prefix on
     * the class, {@code CategoryController} puts it on one method.
     */
    private static boolean isAnAdminHandler(JavaMethod method) {
        List<String> classPaths = mappedPathsOf(method.getOwner());
        List<String> methodPaths = mappedPathsOf(method);
        if (methodPaths.isEmpty() && !isAMapping(method)) {
            return false;
        }
        if (methodPaths.isEmpty()) {
            methodPaths = List.of("");
        }
        List<String> prefixes = classPaths.isEmpty() ? List.of("") : classPaths;

        for (String prefix : prefixes) {
            for (String path : methodPaths) {
                if ((prefix + path).startsWith(ADMIN_PREFIX)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The Spring MVC mapping annotations, read uniformly through their meta-annotation. */
    private static final List<Class<? extends Annotation>> MAPPINGS = List.of(
            RequestMapping.class, GetMapping.class, PostMapping.class,
            PutMapping.class, PatchMapping.class, DeleteMapping.class);

    private static boolean isAMapping(JavaMethod method) {
        return MAPPINGS.stream().anyMatch(mapping ->
                method.tryGetAnnotationOfType(mapping.getName()).isPresent());
    }

    /**
     * The paths a mapping annotation declares, read off the raw annotation properties.
     *
     * <p>{@code value} and {@code path} are aliases; a source may use either, and ArchUnit
     * reports only the member that was written, so both are looked at.
     */
    private static List<String> mappedPathsOf(HasAnnotations<?> element) {
        for (Class<? extends Annotation> mapping : MAPPINGS) {
            Optional<? extends JavaAnnotation<?>> declared =
                    element.tryGetAnnotationOfType(mapping.getName());
            if (declared.isEmpty()) {
                continue;
            }
            List<String> paths = new ArrayList<>();
            for (String member : List.of("value", "path")) {
                if (declared.get().get(member).orElse(null) instanceof String[] values) {
                    paths.addAll(List.of(values));
                }
            }
            if (!paths.isEmpty()) {
                return paths;
            }
        }
        return List.of();
    }

    /**
     * Every type a domain event carries must be readable by a consumer in another context:
     * the JDK, the shared kernel, or an enum from the publisher's own domain.
     */
    private static ArchCondition<JavaClass> carryOnlyPublishableTypes() {
        return new ArchCondition<>("carry only shared-kernel types") {
            @Override
            public void check(JavaClass event, ConditionEvents events) {
                for (JavaField field : event.getAllFields()) {
                    // getType(), not getRawType(). The erasure of a List<OrderLine> field is
                    // java.util.List, which is publishable — the leak is entirely in the type
                    // argument, and that is the exact shape S10's OrderPlaced had.
                    for (JavaClass carried : typesCarriedBy(field.getType())) {
                        if (isPublishable(carried, contextOf(event))) {
                            continue;
                        }
                        events.add(SimpleConditionEvent.violated(event, String.format(
                                "'%s' carries '%s', which is internal to '%s' — a consumer in "
                                        + "another context cannot read it (%s)",
                                event.getSimpleName(), carried.getSimpleName(),
                                contextOf(event), field.getFullName())));
                    }
                }
            }
        };
    }

    private static boolean isPublishable(JavaClass carried, String publisherContext) {
        String name = carried.getPackageName();
        if (name.startsWith("java.") || name.startsWith("javax.") || carried.isPrimitive()) {
            return true;
        }
        if (name.startsWith(ROOT + ".shared.")) {
            return true;
        }
        // A generic type variable erases to Object; the concrete arguments are checked
        // separately by the consumer-side rule, which is where they are actually resolvable.
        if (carried.getName().equals("java.lang.Object")) {
            return true;
        }
        String carriedContext = contextOf(carried);
        return carriedContext != null
                && carriedContext.equals(publisherContext)
                && (carried.isEnum() || carried.isInterface());
    }

    /**
     * A type and everything it wraps: the component type of an array, and the type arguments
     * of a generic. {@code Map<ProductId, Quantity>} yields all three.
     */
    private static java.util.Set<JavaClass> typesCarriedBy(JavaType type) {
        java.util.Set<JavaClass> carried = new java.util.LinkedHashSet<>();
        collectTypes(type, carried);
        return carried;
    }

    private static void collectTypes(JavaType type, java.util.Set<JavaClass> into) {
        if (type instanceof JavaParameterizedType parameterized) {
            into.add(parameterized.toErasure());
            parameterized.getActualTypeArguments().forEach(argument -> collectTypes(argument, into));
            return;
        }
        JavaClass erasure = type.toErasure();
        into.add(erasure.isArray() ? erasure.getBaseComponentType() : erasure);
    }

    /**
     * Every type named in the signature of a port — an interface in some context's
     * {@code domain} package — including the type arguments of a generic one.
     *
     * <p>This is what makes a port callable across a context boundary rather than merely
     * holdable. See the S13 note on
     * {@link #contexts_touch_each_other_only_through_published_types}.
     *
     * <p><b>A port may only publish its own context's types.</b> Without that restriction the
     * clause would be a laundering channel: a port in {@code catalog} that returned an
     * {@code ordering} internal would make that internal nameable by <em>every</em> context,
     * and the leak would be reported nowhere. Restricted this way, such a port is still a
     * violation — reported against the port, which is where the mistake is.
     */
    private static java.util.Set<String> typesNamedByPorts(JavaClasses classes) {
        java.util.Set<String> published = new java.util.HashSet<>();
        for (JavaClass port : classes) {
            String portContext = contextOf(port);
            if (!port.isInterface()
                    || portContext == null
                    || !port.getPackageName().contains(".domain")) {
                continue;
            }
            for (JavaMethod method : port.getMethods()) {
                Stream.concat(Stream.of(method.getReturnType()), method.getParameterTypes().stream())
                        .flatMap(signatureType -> typesCarriedBy(signatureType).stream())
                        .filter(carried -> portContext.equals(contextOf(carried)))
                        .filter(carried -> carried.getPackageName().contains(".domain"))
                        .forEach(carried -> published.add(carried.getFullName()));
            }
        }
        return published;
    }

    /** The shared kernel's exception vocabulary. A context's specializations of these are published. */
    private static final List<String> DOMAIN_EXCEPTION_BASES = List.of(
            ROOT + ".shared.domain.DomainRuleViolationException",
            ROOT + ".shared.domain.DomainNotFoundException",
            ROOT + ".shared.domain.DomainConflictException",
            ROOT + ".shared.domain.AccessDeniedDomainException");

    private static boolean isAPublishedDomainException(JavaClass target) {
        return target.getPackageName().contains(".domain")
                && DOMAIN_EXCEPTION_BASES.stream().anyMatch(target::isAssignableTo);
    }

    private static ArchCondition<JavaClass> onlyTouchOtherContextsThroughPublishedTypes(
            java.util.Set<String> typesNamedByPorts) {
        return new ArchCondition<>(
                "touch other bounded contexts only through a port, a domain event or a published value") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String originContext = contextOf(origin);
                if (originContext == null) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    String targetContext = contextOf(target);
                    if (targetContext == null || targetContext.equals(originContext)) {
                        continue;
                    }
                    if (isPublishedLanguage(target)
                            || isAPublishedDomainException(target)
                            || typesNamedByPorts.contains(target.getFullName())) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(origin, String.format(
                            "'%s' reaches into '%s': %s is not a port, event or published value of that context (%s)",
                            originContext, targetContext, target.getSimpleName(),
                            dependency.getDescription())));
                }
            }
        };
    }

    /** The context a class belongs to, or {@code null} for {@code shared}, {@code platform} and legacy code. */
    private static String contextOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(ROOT + ".")) {
            return null;
        }
        String remainder = packageName.substring(ROOT.length() + 1);
        int dot = remainder.indexOf('.');
        String head = dot < 0 ? remainder : remainder.substring(0, dot);
        return CONTEXTS.contains(head) ? head : null;
    }

    /**
     * A type another context is allowed to name: a port, a domain event, or a published value.
     *
     * <h2>S11: a domain event is recognized by its interface, not by its name</h2>
     * The original wording accepted a type whose simple name ended in {@code Event}, and its
     * own comment admitted the awkwardness — "{@code OrderPlaced} is consumed as
     * {@code OrderPlacedEvent}". That was fine while every listener lived in the same context
     * as the event it consumed, so the clause was never exercised. S11 exercises it: the
     * stock decrement moved to {@code catalog} and the cart clear to {@code cart}, and both
     * consume events {@code ordering} publishes.
     *
     * <p>Naming was the wrong test. {@code OrderPlaced} and {@code OrderCancelled} are
     * published by construction — they implement {@link DomainEvent}, the shared-kernel marker
     * whose whole purpose is to say "this is a value one context announces to others". A
     * convention that a class must also be *called* {@code …Event} adds nothing a reviewer
     * would catch and would have been satisfied by a rename, which is the shape of rule that
     * teaches people to rename things rather than to think.
     *
     * <p>This is a loosening, and worth being explicit about. It is narrower than it looks:
     * the interface is in {@code shared.domain}, so an aggregate cannot acquire published
     * status by accident, and everything an event carries is checked independently — which is
     * why {@code OrderPlaced} had to stop carrying {@code List<OrderLine>} and start carrying
     * {@code Map<ProductId, Quantity>} in the same commit. The name-suffix clause stays as a
     * fallback for a future event that predates the interface.
     */
    private static boolean isPublishedLanguage(JavaClass target) {
        if (!target.getPackageName().contains(".domain")) {
            return false;
        }
        String name = target.getSimpleName();
        return target.isInterface()
                || target.isEnum()
                || target.isAssignableTo(DomainEvent.class)
                || name.endsWith("Event")
                || name.endsWith("Snapshot")
                || name.endsWith("Id");
    }
}
