import jenkins.model.*
import hudson.security.*
import jenkins.security.SecurityListener

// Cr¨¦er un nouvel utilisateur admin
def user = hudson.model.User.get("admin", true)
def password = "admin123"

def details = new hudson.security.HudsonPrivateSecurityRealm.Details(null)
details.passwordHash = hudson.security.HudsonPrivateSecurityRealm.get().getPasswordEncoder().encode(password)
user.addProperty(details)

// Donner les droits d'admin
def strategy = Jenkins.instance.getAuthorizationStrategy()
if (strategy instanceof hudson.security.GlobalMatrixAuthorizationStrategy) {
    strategy.add(hudson.model.Hudson.ADMINISTER, "admin")
} else {
    def newStrategy = new hudson.security.GlobalMatrixAuthorizationStrategy()
    newStrategy.add(hudson.model.Hudson.ADMINISTER, "admin")
    Jenkins.instance.setAuthorizationStrategy(newStrategy)
}

Jenkins.instance.save()
println "Utilisateur 'admin' cr¨¦¨¦ avec mot de passe 'admin123'"
