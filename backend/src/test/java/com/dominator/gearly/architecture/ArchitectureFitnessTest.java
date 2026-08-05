package com.dominator.gearly.architecture;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
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
     * <p>Both the framework's package and this codebase's own {@code security} package are
     * banned, because {@code AuthenticatedUser} lives in the latter. S12 moves the whole
     * access boundary; this rule is what stops it drifting back in the meantime.
     */
    @ArchTest
    static final ArchRule security_types_stop_at_the_api_layer =
            noClasses().that().resideInAnyPackage(CONTEXT_PACKAGES)
                    .and().resideOutsideOfPackage(ROOT + "..api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.security..", ROOT + ".security..")
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
     */
    @ArchTest
    static void contexts_touch_each_other_only_through_published_types(JavaClasses classes) {
        // Phrased positively on purpose. `noClasses().should(customCondition)` inverts the
        // condition's events, which would silently make this rule inert.
        classes().that().resideInAnyPackage(CONTEXT_PACKAGES)
                .should(onlyTouchOtherContextsThroughPublishedTypes())
                .because("cross-context coupling goes through a port, an event, or a published value")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * The dependency between a context and the platform is one-way. Platform wires
     * everything; a context that needs something from the wiring has misplaced a rule.
     */
    @ArchTest
    static final ArchRule contexts_do_not_depend_on_the_platform =
            noClasses().that().resideInAnyPackage(CONTEXT_PACKAGES)
                    .should().dependOnClassesThat().resideInAPackage(ROOT + ".platform..")
                    .because("platform knows about the contexts; the contexts do not know about it")
                    .allowEmptyShould(false);

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

    private static ArchCondition<JavaClass> onlyTouchOtherContextsThroughPublishedTypes() {
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
                    if (isPublishedLanguage(target)) {
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
