import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { User } from '../../common/user';
import { Router, RouterModule } from '@angular/router';
import { MyCustomValidator } from '../../validators/my-custom-validator';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  imports: [
    ReactiveFormsModule,
    RouterModule,
    CommonModule
  ]
})
export class RegisterComponent implements OnInit {
  registerForm: any;

  constructor(private formBuilder: FormBuilder, private authService: AuthService, private router: Router) { }
  ngOnInit(): void {
    this.registerForm = this.formBuilder.nonNullable.group({
      username: ['', [Validators.required, Validators.minLength(3), MyCustomValidator.notOnlyWhitespace]],
      email: ['', [Validators.required, Validators.email, MyCustomValidator.notOnlyWhitespace]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(20), MyCustomValidator.notOnlyWhitespace]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: import('@angular/forms').AbstractControl) {
    const form = control as FormGroup;
    const password = form.get('password')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { mismatch: true };
  }

  onSubmit() {
    const user = new User(
      this.registerForm.value.username,
      this.registerForm.value.email,
      this.registerForm.value.password
    );
    this.authService.register(user).subscribe({
      next: () => { alert('Înregistrare reușită!'); this.router.navigate(["/login"]) },
      error: () => alert('Eroare la înregistrare')
    });

  }
}

