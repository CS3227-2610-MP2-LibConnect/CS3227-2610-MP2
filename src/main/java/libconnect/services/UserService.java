package libconnect.services;

import java.util.Optional;

import libconnect.models.User;
import libconnect.storage.file.FileLibrarianRepository;
import libconnect.storage.file.FileMemberRepository;
import libconnect.storage.repositories.LibrarianRepository;
import libconnect.storage.repositories.MemberRepository;

public class UserService {
    private final MemberRepository memberRepository;
    private final LibrarianRepository librarianRepository;

    public UserService() {
        this.memberRepository = new FileMemberRepository();
        this.librarianRepository = new FileLibrarianRepository();
    }

    public UserService(MemberRepository memberRepository, LibrarianRepository librarianRepository) {
        this.memberRepository = memberRepository;
        this.librarianRepository = librarianRepository;
    }

    public boolean isEmailInUse(String email) {
        return memberRepository.findByEmail(email).isPresent() || librarianRepository.findByEmail(email).isPresent();
    }
    
    public boolean isUserIdInUse(String userId) {
        return memberRepository.findByUserId(userId).isPresent() || librarianRepository.findByUserId(userId).isPresent();
    }

    public Optional<User> findUserByEmail(String email) {
        return memberRepository.findByEmail(email).map(member -> (User) member)
                .or(() -> librarianRepository.findByEmail(email).map(librarian -> (User) librarian));
    }
    
}
